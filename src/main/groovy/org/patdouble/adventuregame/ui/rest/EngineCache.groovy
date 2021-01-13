package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.engine.jdbc.BlobProxy
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.AgendaLog
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.http.HttpStatus
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import org.springframework.web.server.ResponseStatusException

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.PersistenceContext
import javax.persistence.PersistenceException
import javax.transaction.Transactional
import javax.validation.constraints.NotNull
import java.sql.SQLException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPOutputStream

/**
 * Maintains a cache of {@link org.patdouble.adventuregame.engine.Engine} instances to balance performance and memory.
 * This class is thread-safe.
 */
@Component
@Slf4j
@CompileDynamic
class EngineCache {
    private static final long B_PER_MB = 1024 * 1024
    private static final Runtime RT = Runtime.runtime

    @CompileStatic
    private class Value {
        final Engine engine
        final AtomicLong expires = new AtomicLong(0)

        Value(Engine engine) {
            this.engine = engine
            touch()
        }

        void touch() {
            expires.set(System.currentTimeMillis() + ttl.toMillis())
        }
    }

    @NotNull
    Duration ttl = Duration.ofMinutes(10)

    /** Automatically move through the lifecycle. */
    boolean autoLifecycle = true
    @Autowired
    WorldRepository worldRepository
    @Autowired
    StoryRepository storyRepository
    @PersistenceContext
    EntityManager entityManager
    @Autowired
    private EntityManagerFactory entityManagerFactory
    @Autowired(required = false)
    SimpMessageSendingOperations simpMessagingTemplate
    @Autowired
    @Qualifier('application')
    AsyncTaskExecutor executor

    private final ConcurrentMap<UUID, Value> map = new ConcurrentHashMap<>()

    /**
     * Get an engine for the story.
     * @param storyId the ID of the story.
     */
    Engine get(final UUID storyId) {
        Value v = map.computeIfAbsent(storyId) { k ->
            Optional<Story> story = storyRepository.findById(storyId)
            if (!story.present) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND)
            }
            if (story.get().ended) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 'Story is ended')
            }
            Engine engine = configure(new Engine(story.get().initialize(), executor))
            engine.init().join()
            if (engine.story.chronos.current > 0) {
                engine.start().join()
            }
            new Value(engine)
        }
        v.touch()
        v.engine
    }

    /**
     * Create a new story.
     */
    @Transactional
    Engine create(World world) {
        Story story = storyRepository.save(new Story(world)).initialize()
        Objects.requireNonNull(story.id)
        Engine engine = configure(new Engine(story, executor))
        engine.init().join()
        engine.updateStory { storyRepository.saveAndFlush(story) }.join()
        Objects.requireNonNull(story.id)
        map.put(story.id, new Value(engine))
        engine
    }

    CompletableFuture<Void> clear() {
        remove { true }
    }

    /**
     * Expire engines that haven't seen activity since the {@link #ttl}.
     */
    @Transactional
    CompletableFuture<Void> expire(long timeInMillis = System.currentTimeMillis()) {
        remove { v -> v.expires.get() < timeInMillis || v.engine.story.ended }
    }

    @Transactional
    CompletableFuture<Void> sweep() {
        List<CompletableFuture<?>> futures = []

        Iterator<Value> iter = map.values().iterator()
        while (iter.hasNext()) {
            Value v = iter.next()
            futures << v.engine.updateStory {
                Story result = null
                try {
                    doInTransaction { EntityManager em ->
                        result = em.merge(v.engine.story).initialize()
                        em.flush()
                    }
                } catch (PersistenceException | SQLException e) {
                    log.error("Saving story ${v.engine.story.id}, removing from cache", e)
                    iter.remove()
                }
                result
            }
        }
        CompletableFuture.allOf(futures as CompletableFuture[])
    }

    int size() {
        map.size()
    }

    private Engine configure(Engine engine) {
        engine.kieRuntimeLoggerFile = File.createTempFile('story-agenda', '.xml')
        if (simpMessagingTemplate != null) {
            engine.subscribe(new EngineFlowToSpringMessagingAdapter(engine.story.id, simpMessagingTemplate))
        }
        engine.autoLifecycle = this.autoLifecycle
        engine
    }

    /**
     * Remove the engines matching the condition. The argument to the closure is the Value. This method is thread-safe.
     */
    private CompletableFuture<Void> remove(Closure<Boolean> condition) {
        List<CompletableFuture<Void>> futures = []

        Iterator<Value> i = map.values().iterator()
        while (i.hasNext()) {
            Value v = i.next()
            if (!condition.call(v)) {
                continue
            }

            // remove it before we start closing so no one else will use it
            i.remove()

            final Story s = v.engine.story
            long age = System.currentTimeMillis() - v.expires.get()
            futures << v.engine.close().thenRun {
                doInTransaction { EntityManager em ->
                    if (!em.contains(s)) {
                        em.merge(s)
                    }
                    saveEngineLog(v.engine, em)
                    em.flush()
                }
                log.info('Closed Engine for story {}, age {}, ended {}', s.id, age, s.ended)
            }

            log.info('Removed Engine for story {}, age {}, ended {}', s.id, age, s.ended)
        }

        CompletableFuture.allOf(futures as CompletableFuture[])
    }

    private void doInTransaction(Closure c) {
        if (entityManager.isJoinedToTransaction()) {
            c.call(entityManager)
        } else {
            EntityManager em = entityManagerFactory.createEntityManager()
            EntityTransaction tx = em.getTransaction()
            boolean success = false
            try {
                tx.begin()
                c.call(em)
                success = true
            } finally {
                if (success) {
                    tx.commit()
                } else {
                    tx.rollback()
                }
                em.close()
            }
        }
    }
    private void saveEngineLog(Engine engine, EntityManager em) {
        Story story = engine.story
        File file = engine.kieRuntimeLoggerFile
        if (file == null) {
            log.debug 'Skipping agenda log, no file configured'
            return
        }
        if (!file.exists() || file.length() == 0) {
            // Drools may append '.log' to the filename
            File file2 = new File(file.parentFile, file.name + '.log')
            if (file2.exists()) {
                file.delete()
                file = file2
            }
        }
        if (!file.exists() || file.length() == 0) {
            log.debug 'Skipping agenda log, nothing to log, file = {}, exists = {}, length = {}',
                    file?.absolutePath, file?.exists(), file?.length()
            file.delete()
            return
        }
        File compressed = File.createTempFile(file.name, '.gz')
        log.debug 'Compressing agenda log {} to {}', file, compressed
        GZIPOutputStream gzout
        FileInputStream fin
        boolean compressSuccess = false
        try {
            gzout = new GZIPOutputStream(new FileOutputStream(compressed))
            fin = new FileInputStream(file)
            StreamUtils.copy(fin, gzout)
            compressSuccess = true
        } finally {
            if (gzout) {
                gzout.close()
            }
            if (fin) {
                fin.close()
            }
        }
        if (!compressSuccess) {
            log.debug 'Compression failed'
            compressed.delete()
            file.delete()
            return
        }

        try {
            log.debug 'Creating AgendaLog for story {}', story.id
            AgendaLog agendaLog = new AgendaLog()
            agendaLog.story = engine.story
            agendaLog.gzlog = BlobProxy.generateProxy(new FileInputStream(compressed), compressed.length())
            log.debug 'Persisting AgendaLog for story {}', story.id
            em.persist(agendaLog)
        } finally {
            compressed.delete()
            file.delete()
        }
    }

    @Scheduled(fixedDelay = 30000L)
    @Transactional
    void scheduleSweep() {
        log.info('Starting sweep with {} engines, {} pending close, memory total/max/free {}M/{}M/{}M',
                map.size(),
                Engine.PENDING_CLOSE.get(),
                Math.floor(RT.totalMemory() / B_PER_MB),
                Math.floor(RT.maxMemory() / B_PER_MB),
                Math.floor(RT.freeMemory() / B_PER_MB))

        expire()
        sweep()

        log.info('Finished sweep with {} engines, {} pending close, memory total/max/free {}M/{}M/{}M',
                map.size(),
                Engine.PENDING_CLOSE.get(),
                Math.floor(RT.totalMemory() / B_PER_MB),
                Math.floor(RT.maxMemory() / B_PER_MB),
                Math.floor(RT.freeMemory() / B_PER_MB))
    }
}

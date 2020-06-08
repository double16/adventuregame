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
import javax.transaction.Transactional
import javax.validation.constraints.NotNull
import java.time.Duration
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
    private static long B_PER_MB = 1024*1024
    private static Runtime RT = Runtime.runtime

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
        engine.updateStory { storyRepository.saveAndFlush(story) }
        map.put(story.id, new Value(engine))
        engine
    }

    void clear() {
        remove({true})
    }

    /**
     * Expire engines that haven't seen activity since the {@link #ttl}.
     */
    @Transactional
    void expire(long timeInMillis = System.currentTimeMillis()) {
        remove({ v -> v.expires.get() < timeInMillis || v.engine.story.ended })
    }

    @Transactional
    void sweep() {
        map.values().each { Value v ->
            v.engine.updateStory {
                EntityManager em = entityManagerFactory.createEntityManager()
                EntityTransaction tx = em.getTransaction()
                Story result = null
                boolean success = false
                try {
                    tx.begin()
                    result = em.merge(v.engine.story).initialize()
                    em.flush()
                    success = true
                } catch (RuntimeException e) {
                    log.error("Saving story ${v.engine.story.id}", e)
                } finally {
                    if (success) {
                        tx.commit()
                    } else {
                        tx.rollback()
                    }
                    em.close()
                }
                result
            }
        }
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
    private void remove(Closure<Boolean> condition) {
        Iterator<Value> i = map.values().iterator()
        while (i.hasNext()) {
            Value v = i.next()
            if (!condition.call(v)) {
                continue
            }

            // remove it before we start closing so no one else will use it
            i.remove()
            v.engine.close()

            Story s = v.engine.story
            if (!entityManager.contains(s)) {
                entityManager.merge(s)
            }
            saveEngineLog(v.engine)
            entityManager.flush()
            log.info('Removed Engine for story {}, age {}, ended {}', s.id, System.currentTimeMillis() - v.expires.get(), s.ended)
        }
    }

    private void saveEngineLog(Engine engine) {
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
            log.debug 'Skipping agenda log, nothing to log, file = {}, exists = {}, length = {}', file?.absolutePath, file?.exists(), file?.length()
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
            entityManager.persist(agendaLog)
        } finally {
            compressed.delete()
            file.delete()
        }
    }

    @Scheduled(fixedDelay = 30000L)
    @Transactional
    void scheduleSweep() {
        log.info('Starting sweep with {} engines, memory total/max/free {}M/{}M/{}M',
                map.size(),
                Math.floor(RT.totalMemory()/B_PER_MB),
                Math.floor(RT.maxMemory()/B_PER_MB),
                Math.floor(RT.freeMemory()/B_PER_MB))

        sweep()
        expire()

        log.info('Finished sweep with {} engines, memory total/max/free {}M/{}M/{}M',
                map.size(),
                Math.floor(RT.totalMemory()/B_PER_MB),
                Math.floor(RT.maxMemory()/B_PER_MB),
                Math.floor(RT.freeMemory()/B_PER_MB))
    }
}

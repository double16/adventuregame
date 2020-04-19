package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.validation.constraints.NotNull
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Maintains a cache of {@link org.patdouble.adventuregame.engine.Engine} instances to balance performance and memory.
 * This class is thread-safe.
 */
@Component
@CompileDynamic
class EngineCache {

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

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private ScheduledFuture scheduledFuture

    @NotNull
    Duration ttl = Duration.ofMinutes(10)
    @NotNull
    Duration sweepInterval = Duration.ofMinutes(20)
    /** Automatically move through the lifecycle. */
    boolean autoLifecycle = true
    @Autowired
    WorldRepository worldRepository
    @Autowired
    StoryRepository storyRepository
    @PersistenceContext
    EntityManager entityManager
    @Autowired(required = false)
    SimpMessageSendingOperations simpMessagingTemplate

    private final ConcurrentMap<UUID, Value> map = new ConcurrentHashMap<>()

    EngineCache() {
        scheduleSweep()
    }

    void setSweepInterval(@NotNull Duration d) {
        Objects.requireNonNull(d)
        if (d != sweepInterval) {
            sweepInterval = d
            scheduleSweep()
        }
    }

    private Engine configure(Engine engine) {
        if (simpMessagingTemplate != null) {
            engine.subscribe(new EngineFlowToSpringMessagingAdapter(engine.story.id, simpMessagingTemplate))
        }
        engine.autoLifecycle = this.autoLifecycle
        engine
    }

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
            Engine engine = configure(new Engine(story.get()))
            engine.init()
            if (engine.story.chronos.current > 0) {
                engine.start()
            }
            new Value(engine)
        }
        v.touch()
        v.engine
    }

    /**
     * Create a new story.
     */
    Engine create(World world) {
        Story story = storyRepository.save(new Story(world))
        Objects.requireNonNull(story.id)
        Engine engine = configure(new Engine(story))
        engine.init()
        engine.story = storyRepository.saveAndFlush(story)
        map.put(story.id, new Value(engine))
        engine
    }

    void clear() {
        remove(map.values())
    }

    /**
     * Expire engines that haven't seen activity since the {@link #ttl}.
     */
    void expire(long timeInMillis = System.currentTimeMillis()) {
        remove(map.values().findAll { it.expires.get() > timeInMillis })
    }

    void sweep() {
        map.values().each { Value v ->
//            entityManager.merge(v.engine.story)
            v.engine.story = storyRepository.save(v.engine.story)
        }
        storyRepository.flush()
    }

    int size() {
        map.size()
    }

    /**
     * Remove the engines contained in the Iterable. This method is thread-safe.
     * The Iterable should also be thread-safe.
     */
    private void remove(Iterable<Value> engines) {
        Iterator<Value> i = engines.iterator()
        while (i.hasNext()) {
            Value v = i.next()
            // remove it before we start closing so no one else will use it
            i.remove()
//            entityManager.merge(v.engine.story)
            v.engine.story = storyRepository.save(v.engine.story)
            v.engine.close()
        }
        storyRepository.flush()
    }

    private void scheduleSweep() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false)
        }
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate({
            sweep()
            expire()
        },
                sweepInterval.toMillis(),
                sweepInterval.toMillis(),
                TimeUnit.MILLISECONDS)
    }
}

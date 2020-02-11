package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

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
    StoryRepository storyRepository
    @Autowired(required = false)
    SimpMessagingTemplate simpMessagingTemplate

    private final ConcurrentMap<UUID, Value> map = new ConcurrentHashMap<>()

    EngineCache() {
        scheduleSweep()
    }

    void setSweepInterval(@NotNull Duration d) {
        Objects.nonNull(d)
        if (d != sweepInterval) {
            sweepInterval = d
            scheduleSweep()
        }
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
            Engine engine = new Engine(story.get())
            if (simpMessagingTemplate != null) {
                engine.subscribe(new EngineFlowToSpringMessagingAdapter(engine.story.id, simpMessagingTemplate))
            }
            engine.autoLifecycle = this.autoLifecycle
            engine.init()
            if (engine.story.chronos.current > 0) {
                engine.start()
            }
            new Value(engine)
        }
        v.touch()
        v.engine
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
            storyRepository.save(v.engine.story)
        }
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
            storyRepository.save(v.engine.story)
            v.engine.close()
        }
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

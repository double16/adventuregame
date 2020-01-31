package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

import javax.validation.constraints.NotNull
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
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

    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private ScheduledFuture scheduledFuture

    @NotNull
    Duration ttl = Duration.ofMinutes(10)
    @NotNull
    Duration sweepInterval = Duration.ofMinutes(20)
    @Autowired
    StoryRepository storyRepository
    private final ConcurrentHashMap<UUID, Value> map = new ConcurrentHashMap<>()

    EngineCache() {
        scheduleSweep()
    }

    /**
     * Remove the engines contained in the Iterable. This method is thread-safe. The Iterable should also be thread-safe.
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

    private scheduleSweep() {
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

    void setSweepInterval(@NotNull Duration d) {
        Objects.nonNull(d)
        if (d != sweepInterval) {
            sweepInterval = d
            scheduleSweep()
        }
    }

    Engine get(final UUID storyId) {
        Value v = map.computeIfAbsent(storyId, { k ->
            Optional<Story> story = storyRepository.findById(storyId)
            if (!story.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND)
            }
            if (story.get().ended) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 'Story is ended')
            }
            Engine engine = new Engine(story.get())
            engine.autoLifecycle = true
            engine.init()
            if (engine.story.chronos.current > 0) {
                engine.start()
            }
            new Value(engine)
        })
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
}

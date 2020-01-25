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

    @NotNull
    Duration ttl = Duration.ofMinutes(10)
    @Autowired
    StoryRepository storyRepository
    private final ConcurrentHashMap<UUID, Value> map = new ConcurrentHashMap<>()

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
        Iterator<Value> i = map.values().iterator()
        while (i.hasNext()) {
            Value v = i.next()
            // remove it before we start closing so no one else will use it
            i.remove()
            storyRepository.save(v.engine.story)
            v.engine.close()
        }
    }
}

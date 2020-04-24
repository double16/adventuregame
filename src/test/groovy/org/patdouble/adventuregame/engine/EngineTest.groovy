package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import spock.lang.Specification

import java.util.concurrent.Executor
import java.util.concurrent.Flow

abstract class EngineTest extends Specification {
    Story story
    Flow.Subscriber<StoryMessage> storySubscriber
    Executor executor
    Engine engine

    void modify(World world) {
        // modify the world for the test case
    }

    def setup() {
        World world = new YamlUniverseRegistry().worlds.find { it.name == YamlUniverseRegistry.TRAILER_PARK }
        modify(world)
        story = new Story(world)

        storySubscriber = Mock()
        storySubscriber.onSubscribe(_) >> { Flow.Subscription s -> s.request(Integer.MAX_VALUE) }
        executor = { it.run() } as Executor

        engine = new Engine(story, executor)
        engine.chronosLimit = 100
        engine.subscribe(storySubscriber)
//        engine.subscribe(new StoryMessageOutput())
    }

    def cleanup() {
        engine.close()
    }
}

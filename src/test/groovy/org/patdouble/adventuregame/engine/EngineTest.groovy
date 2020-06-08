package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.FlowSubscriptionCounter
import org.patdouble.adventuregame.SpecHelper
import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import spock.lang.Specification

import java.util.concurrent.Executor
import java.util.concurrent.Flow

abstract class EngineTest extends Specification {
    Story story
    Flow.Subscriber<StoryMessage> storySubscriber
    FlowSubscriptionCounter subscriptionCounter
    Engine engine

    void modify(World world) {
        // modify the world for the test case
    }

    /**
     * Immobilize AI to provide a more predictable run. Defaults to true.
     */
    boolean immobilizeAI() {
        true
    }

    def setup() {
        World world = new LuaUniverseRegistry().worlds.find { it.name == LuaUniverseRegistry.TRAILER_PARK }
        modify(world)
        if (immobilizeAI()) {
            world.extras.each { it.goals.clear() }
        }
        story = new Story(world)

        storySubscriber = Mock()
        storySubscriber.onSubscribe(_) >> { Flow.Subscription s -> s.request(Integer.MAX_VALUE) }

        subscriptionCounter = new FlowSubscriptionCounter()

        engine = new Engine(story, null, { it.run() } as Executor)
        engine.chronosLimit = 100
        engine.subscribe(storySubscriber)
        engine.subscribe(subscriptionCounter)
//        engine.subscribe(new StoryMessageOutput())
    }

    def cleanup() {
        engine.close()
    }

    /**
     * Wait for messages to settle.
     */
    protected void waitForMessages() {
        SpecHelper.settle(subscriptionCounter.dataHash())
    }
}

package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import spock.lang.Specification

import java.util.concurrent.Flow

class EngineAutostartTest extends Specification {
    Story story
    Flow.Subscriber<StoryMessage> storySubscriber
    Engine engine

    def setup() {
        World world = new LuaUniverseRegistry().worlds.find { it.name == LuaUniverseRegistry.TRAILER_PARK }
        story = new Story(world)

        storySubscriber = Mock()
        storySubscriber.onSubscribe(_) >> { Flow.Subscription s -> s.request(Integer.MAX_VALUE) }

        engine = new Engine(story)
        engine.chronosLimit = 100
        engine.subscribe(storySubscriber)
//        engine.subscribe(new StoryMessageOutput())
    }

    def cleanup() {
        engine.close()
    }

    def "autostart"() {
        given:
        engine.autoLifecycle = true
        engine.init().join()
        engine.story.goals.each { it.goal.required = false }
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thiefRequest = story.requests.find { it.template.persona.name == 'thief' } as PlayerRequest

        when:
        engine.addToCast(warriorRequest.template.createPlayer(Motivator.AI)).join()
        engine.addToCast(thiefRequest.template.createPlayer(Motivator.AI)).join()
        story.requests.findAll { it.template.persona.name == 'thug' }. each {
            engine.addToCast(it.template.createPlayer(Motivator.AI)).join()
        }

        then:
        story.requests.isEmpty()
        !engine.isClosed()
        !story.goals.isEmpty()

        when:
        engine.close().join()

        then:
        engine.isClosed()
    }
}

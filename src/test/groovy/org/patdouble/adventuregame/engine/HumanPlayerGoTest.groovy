package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.request.ActionRequest

class HumanPlayerGoTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "valid go action"() {
        given:
        engine.start().join()

        when:
        boolean success = engine.action(warrior, 'go north').join()

        then: 'state is correct'
        success
        warrior.chronos == 1
        warrior.room.modelId == 'trailer_2'
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and: 'events sent'
        1 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext{ it instanceof RequestSatisfied && it.request.player == warrior }
    }

    def "valid go action - abbreviated"() {
        given:
        engine.start().join()

        when:
        boolean success = engine.action(warrior, 'go n').join()

        then:
        success
        warrior.chronos == 1
        warrior.room.modelId == 'trailer_2'
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext{ it instanceof RequestSatisfied && it.request.player == warrior }
    }

    def "multiple go directions - abbreviated"() {
        given:
        engine.start().join()
        warrior.room = story.world.rooms.find { it.modelId == 'dump' }

        when:
        boolean success = engine.action(warrior, 'go d').join()

        then:
        !success
        warrior.chronos == 0
        warrior.room.modelId == 'dump'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }
    }

    def "invalid go action"() {
        given:
        engine.start().join()

        when: 'no direction'
        boolean success = engine.action(warrior, 'go').join()
        then:
        !success
        warrior.chronos == 0
        warrior.room.modelId == 'entrance'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }

        when: 'known but invalid direction'
        success = engine.action(warrior, 'go south').join()
        then:
        !success
        warrior.chronos == 0
        warrior.room.modelId == 'entrance'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }

        when: 'unknown direction'
        success = engine.action(warrior, 'go northwest').join()
        then:
        !success
        warrior.chronos == 0
        warrior.room.modelId == 'entrance'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }
    }

    def "valid go actions trigger next"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'go north').join()
        engine.action(thief, 'go north').join()

        then:
        warrior.chronos == 1
        thief.chronos == 1
        warrior.room.modelId == 'trailer_2'
        thief.room.modelId == 'trailer_2'
        !story.requests.find { it instanceof ActionRequest && it.chronos == 1 }
        story.requests.findAll { it instanceof ActionRequest && it.chronos == 2 }.size() == 2
        with(story.requests.find { it instanceof ActionRequest && it.player == warrior }) {
            roomSummary.description == 'Trailer 2. Paths go east, south or west.'
            roomSummary.occupants == 'Victor the thief is here with you.'
            actions == engine.actionStatementParser.availableActions
            directions == [ 'east', 'south', 'west' ]
        }
    }
}

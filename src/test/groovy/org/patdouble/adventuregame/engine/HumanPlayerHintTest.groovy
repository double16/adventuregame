package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.request.ActionRequest

class HumanPlayerHintTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "valid hint action"() {
        given:
        engine.start().join()

        when:
        boolean success = engine.action(warrior, 'hint').join()

        then: 'state is correct'
        success
        warrior.chronos == 0
        warrior.room.modelId == 'entrance'
        warrior.motivator == Motivator.HUMAN
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and: 'events sent'
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        2 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }
    }

    def "valid hint action does not affect play"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'hint').join()
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
            actions == ['escape', 'exit', 'flee', 'go', 'hint', 'leave', 'map', 'move', 'run', 'stay', 'swim', 'wait']
            directions == [ 'east', 'south', 'west' ]
        }
    }
}

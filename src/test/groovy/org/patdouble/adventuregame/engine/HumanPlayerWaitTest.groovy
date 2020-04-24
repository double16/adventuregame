package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.request.ActionRequest

class HumanPlayerWaitTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "valid wait action"() {
        given:
        engine.start().join()

        when:
        boolean success = engine.action(warrior, 'wait').join()

        then:
        success
        warrior.chronos == 1
        warrior.room.modelId == 'entrance'
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext{ it instanceof RequestSatisfied && it.request.player == warrior }
    }

    def "valid wait actions trigger next"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'wait').join()
        engine.action(thief, 'wait').join()

        then:
        warrior.chronos == 1
        thief.chronos == 1
        warrior.room.modelId == 'entrance'
        thief.room.modelId == 'entrance'
        !story.requests.find { it instanceof ActionRequest && it.chronos == 1 }
        story.requests.findAll { it instanceof ActionRequest && it.chronos == 2 }.size() == 2
        with(story.requests.find { it instanceof ActionRequest && it.player == warrior }) {
            roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
            roomSummary.occupants == 'Victor the thief and 3 thugs are here with you.'
            actions == engine.actionStatementParser.availableActions
            directions == [ 'north' ]
        }
    }
}

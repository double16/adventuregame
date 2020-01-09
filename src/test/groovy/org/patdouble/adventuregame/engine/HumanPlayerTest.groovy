package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.request.ActionRequest

class HumanPlayerTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "initial actions"() {
        when:
        engine.start()

        then:
        story.requests.count { it instanceof ActionRequest } == 2
        with(story.requests.find { it instanceof ActionRequest && it.player == warrior }) {
            roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
            roomSummary.occupants == 'Victor the thief and 3 thugs are here with you.'
            actions == engine.actionStatementParser.availableActions
            directions == [ 'north' ]
        }
        with(story.requests.find { it instanceof ActionRequest && it.player == thief }) {
            roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
            roomSummary.occupants == 'Shadowblow the warrior and 3 thugs are here with you.'
            actions == engine.actionStatementParser.availableActions
            directions == [ 'north' ]
        }
    }

    def "initial extras placement"() {
        when:
        engine.start()

        then:
        story.cast.findAll { it.persona.name == 'thug' && it.room.id == 'entrance' }.size() == 3
        story.cast.findAll { it.persona.name == 'thug' && it.room.id == 'dump' }.size() == 5

        and:
        story.roomSummary(story.world.rooms.find { it.id == 'entrance' }, warrior, engine.bundles).occupants == 'Victor the thief and 3 thugs are here with you.'
        story.roomSummary(story.world.rooms.find { it.id == 'dump' }, warrior, engine.bundles).occupants == '5 thugs are here with you.'
        !story.roomSummary(story.world.rooms.find { it.id == 'trailer_1' }, warrior, engine.bundles).occupants
    }

    def "roomSummary human players no extras"() {
        given:
        story.world.extras.clear()
        engine.start()

        when:
        RoomSummary roomSummary = ((ActionRequest) story.requests.find { it instanceof ActionRequest && it.player == warrior }).roomSummary

        then:
        roomSummary.name == 'Entrance'
        roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
        roomSummary.chronos == 1
        roomSummary.occupants == "${thief.getTitle()} is here with you."
    }

    def "roomSummary human players with extras"() {
        given:
        engine.start()

        when:
        RoomSummary roomSummary = ((ActionRequest) story.requests.find { it instanceof ActionRequest && it.player == warrior }).roomSummary

        then:
        roomSummary.name == 'Entrance'
        roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
        roomSummary.chronos == 1
        roomSummary.occupants == "${thief.getTitle()} and 3 thugs are here with you."
    }

    def "roomSummary human players no occupants"() {
        given:
        engine.start()

        when:
        engine.action(warrior, 'go north')
        engine.action(thief, 'go north')
        engine.action(warrior, 'go west')
        engine.action(thief, 'go east')

        then:
        thief.room.id == 'trailer_3'
        with(story.requests.find { it instanceof ActionRequest && it.player == thief }) {
            roomSummary.description == 'Trailer 3. Paths go east or west.'
            !roomSummary.occupants
            actions == engine.actionStatementParser.availableActions
            directions == [ 'east', 'west' ]
        }
    }

    def "action without request"() {
        given:
        engine.start()

        when:
        engine.action(warrior, 'wait')
        boolean success = engine.action(warrior, 'wait')

        then:
        !success
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerNotification(warrior,
                'Not yet your turn',
                'You tried to do something but it\'s not your turn. This is usually a logic error in the application.'))

    }

    def "custom action not implemented"() {
        given:
        engine.start()

        when:
        boolean success = engine.action(warrior, 'wait4')

        then:
        !success
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerNotification(warrior,
                'I don\'t understand what you want to do',
                'Things you can do: arrest, attack, buy, capture, drop, escape, exit, fight, flee, go, leave, look, move, nap, pay, pick up, put down, release, rest, run, say, see, sleep, speak, stay, swim, take, talk, wait, wake'))

    }
}

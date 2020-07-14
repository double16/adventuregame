package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.request.ActionRequest
import org.patdouble.adventuregame.state.request.PlayerRequest

class HumanPlayerTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "initial actions"() {
        when:
        engine.start().join()

        then:
        story.requests.count { it instanceof ActionRequest } == 2
        with(story.requests.find { it instanceof ActionRequest && it.player == warrior }) {
            roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
            roomSummary.occupants == 'Victor the thief and 3 thugs are here with you.'
            actions == ['escape', 'exit', 'flee', 'go', 'hint', 'leave', 'map', 'move', 'run', 'stay', 'swim', 'wait']
            directions == [ 'north' ]
        }
        with(story.requests.find { it instanceof ActionRequest && it.player == thief }) {
            roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
            roomSummary.occupants == 'Shadowblow the warrior and 3 thugs are here with you.'
            actions == ['escape', 'exit', 'flee', 'go', 'hint', 'leave', 'map', 'move', 'run', 'stay', 'swim', 'wait']
            directions == [ 'north' ]
        }
    }

    def "initial extras placement"() {
        when:
        engine.start().join()

        then:
        story.cast.findAll { it.persona.name == 'thug' && it.room.modelId == 'entrance' }.size() == 3
        story.cast.findAll { it.persona.name == 'thug' && it.room.modelId == 'dump' }.size() == 5

        and:
        story.roomSummary(story.world.rooms.find { it.modelId == 'entrance' }, warrior, engine.bundles).occupants == 'Victor the thief and 3 thugs are here with you.'
        story.roomSummary(story.world.rooms.find { it.modelId == 'dump' }, warrior, engine.bundles).occupants == '5 thugs are here with you.'
        !story.roomSummary(story.world.rooms.find { it.modelId == 'trailer_1' }, warrior, engine.bundles).occupants
    }

    def "multiple for persona"() {
        when:
        story.requests.findAll { it instanceof PlayerRequest }.each { PlayerRequest r ->
            engine.addToCast(r.template.createPlayer(Motivator.HUMAN)).join()
        }
        engine.start().join()

        then:
        story.cast.count { it.motivator == Motivator.HUMAN } == 12
        story.cast.find { it.persona.name == PersonaMocks.THIEF.name && it.siblingNumber == 1 }
        story.cast.find { it.persona.name == PersonaMocks.WARRIOR.name && it.siblingNumber == 1 }
        story.cast.find { it.persona.name == 'thug' && it.siblingNumber == 1 }
        story.cast.find { it.persona.name == 'thug' && it.siblingNumber == 2 }
        story.cast.find { it.persona.name == 'thug' && it.siblingNumber == 10 }
        and:
        story.requests.count { it instanceof ActionRequest } == 12
    }

    def "roomSummary human players no extras"() {
        given:
        story.world.extras.clear()
        engine.start().join()

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
        engine.start().join()

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
        engine.start().join()

        when:
        engine.action(warrior, 'go north').join()
        engine.action(thief, 'go north').join()
        engine.action(warrior, 'go west').join()
        engine.action(thief, 'go east').join()
        waitForMessages()

        then:
        thief.room.modelId == 'trailer_3'
        with(story.requests.find { it instanceof ActionRequest && it.player == thief }) {
            roomSummary.description == 'Trailer 3. Paths go east or west.'
            !roomSummary.occupants
            actions == ['escape', 'exit', 'flee', 'go', 'hint', 'leave', 'map', 'move', 'run', 'stay', 'swim', 'wait']
            directions == [ 'east', 'west' ]
        }
    }

    def "action without request"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'wait').join()
        boolean success = engine.action(warrior, 'wait').join()
        waitForMessages()

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
        engine.start().join()

        when:
        boolean success = engine.action(warrior, 'wait4').join()
        waitForMessages()

        then:
        !success
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerNotification(warrior,
                'I don\'t understand what you want to do',
                'Things you can do: escape, exit, flee, go, hint, leave, map, move, run, stay, swim, wait'))

    }
}

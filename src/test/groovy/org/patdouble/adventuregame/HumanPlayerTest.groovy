package org.patdouble.adventuregame

import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.ActionRequest
import org.patdouble.adventuregame.state.request.PlayerRequest

class HumanPlayerTest extends EngineTest {
    Player warrior
    Player thief

    def setup() {
        engine.init()
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thiefRequest = story.requests.find { it.template.persona.name == 'thief' } as PlayerRequest
        warrior = warriorRequest.template.createPlayer(Motivator.HUMAN)
        thief = thiefRequest.template.createPlayer(Motivator.HUMAN)
        engine.addToCast(warrior)
        engine.addToCast(thief)
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

    def "valid go action"() {
        given:
        engine.start()

        when:
        boolean success = engine.action(warrior, 'go north')

        then:
        success
        warrior.room.id == 'trailer_2'
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext{ it instanceof RequestSatisfied && it.request.player == warrior }
    }

    def "valid go action - abbreviated"() {
        given:
        engine.start()

        when:
        boolean success = engine.action(warrior, 'go n')

        then:
        success
        warrior.room.id == 'trailer_2'
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        1 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext{ it instanceof RequestSatisfied && it.request.player == warrior }
    }

    def "multiple go directions - abbreviated"() {
        given:
        engine.start()
        warrior.room = story.world.rooms.find { it.id == 'dump' }

        when:
        boolean success = engine.action(warrior, 'go d')

        then:
        !success
        warrior.room.id == 'dump'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }
    }

    def "invalid go action"() {
        given:
        engine.start()

        when: 'no direction'
        boolean success = engine.action(warrior, 'go')
        then:
        !success
        warrior.room.id == 'entrance'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }

        when: 'known but invalid direction'
        success = engine.action(warrior, 'go south')
        then:
        !success
        warrior.room.id == 'entrance'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }

        when: 'unknown direction'
        success = engine.action(warrior, 'go northwest')
        then:
        !success
        warrior.room.id == 'entrance'
        story.requests.find { it instanceof ActionRequest && it.player == warrior }
        and:
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 1))
        1 * storySubscriber.onNext({ it instanceof PlayerNotification && it.player == warrior })
        1 * storySubscriber.onNext{ it instanceof RequestCreated && it.request.player == warrior }
    }

    def "valid go actions trigger next"() {
        given:
        engine.start()

        when:
        engine.action(warrior, 'go north')
        engine.action(thief, 'go north')

        then:
        warrior.room.id == 'trailer_2'
        thief.room.id == 'trailer_2'
        !story.requests.find { it instanceof ActionRequest && it.chronos == 1 }
        story.requests.findAll { it instanceof ActionRequest && it.chronos == 2 }.size() == 2
        with(story.requests.find { it instanceof ActionRequest && it.player == warrior }) {
            roomSummary.description == 'Trailer 2. Paths go east, south or west.'
            roomSummary.occupants == 'Victor the thief is here with you.'
            actions == engine.actionStatementParser.availableActions
            directions == [ 'east', 'south', 'west' ]
        }
    }

    def "roomSummary human players no extras"() {
        given:
        story.world.extras.clear()
        engine.start()

        when:
        RoomSummary roomSummary = ((ActionRequest) story.requests.find { it instanceof ActionRequest }).roomSummary

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
        RoomSummary roomSummary = ((ActionRequest) story.requests.find { it instanceof ActionRequest }).roomSummary

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

    def "look actions"() {

    }
}

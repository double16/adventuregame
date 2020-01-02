package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.request.ActionRequest

class AIPlayerTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.AI
    }

    @Override
    void modify(World world) {
        world.goals.each { it.required = false }
    }

    def setup() {
        engine.chronosLimit = 5
    }

    def "no initial actions"() {
        when:
        engine.start()

        then:
        story.requests.count { it instanceof ActionRequest } == 0
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

    def "roomSummary ai players no extras"() {
        given:
        story.world.extras.clear()
        engine.start()

        when:
        RoomSummary roomSummary = story.roomSummary(story.world.rooms.find { it.id == 'entrance' }, warrior, engine.bundles)

        then:
        roomSummary.name == 'Entrance'
        roomSummary.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes. Paths go north.'
        roomSummary.chronos == 1
        roomSummary.occupants == "${thief.getTitle()} is here with you."
    }

   def "ai action requires no request"() {
        given:
        engine.start()

        when:
        boolean success = engine.action(warrior, 'wait')

        then:
        success
        !story.requests.find { it instanceof ActionRequest && it.player == warrior }
    }
}

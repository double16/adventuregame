package org.patdouble.adventuregame.engine.state

import org.patdouble.adventuregame.engine.AbstractPlayerTest
import org.patdouble.adventuregame.state.Motivator

class HelpersTest extends AbstractPlayerTest {

    @Override
    Motivator getDefaultMotivator() {
        Motivator.AI
    }

    def "ExtractRoomModelId"() {
        expect:
        Helpers.extractRoomModelId('player goes to room "dump"') == ['dump'] as String[]
    }

    def "stringify object HasHumanString"() {
        given:
        RoomObjective obj = new RoomObjective(engine.story.cast.first().id, engine.story.world.rooms.first().id, 1)
        expect:
        Helpers.stringify(engine).call(obj) ==~ /RoomObjective\(.*, to .*, cost 1\)/
    }

    def "stringify object without HasHumanString"() {
        expect:
        Helpers.stringify(engine).call('a') == 'a'
    }

    def "stringify collection"() {
        given:
        RoomObjective obj = new RoomObjective(engine.story.cast.first().id, engine.story.world.rooms.first().id, 1)
        expect:
        Helpers.stringify(engine).call([obj, 'a', 1]) ==~ /\[RoomObjective\(.*, to .*, cost 1\), a, 1]/

    }

    def "stringify RoomExploreObjective"() {
        given:
        RoomExploreObjective obj = new RoomExploreObjective(
                engine.story.cast.first().id,
                engine.story.world.rooms.first().id,
                'north',
                engine.story.world.rooms.first().id,
                engine.story.world.rooms.first().id,
        )
        expect:
        Helpers.stringify(engine).call(obj) ==~ /RoomExploreObjective\(.*, from .* to .* via north to find .*\)/
    }
}

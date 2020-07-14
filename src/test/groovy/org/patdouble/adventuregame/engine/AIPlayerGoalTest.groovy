package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest

import static org.patdouble.adventuregame.SpecHelper.settle

class AIPlayerGoalTest extends EngineTest {
    Player warrior
    Player thief

    @Override
    void modify(World world) {
        world.goals.each {
            it.required = false
            it.theEnd = false
        }
// Uncomment the following to make it easier to debug
//        world.extras.removeIf { it.room.modelId == 'dump' }
//        world.extras*.quantity = 1
    }

    @Override
    boolean immobilizeAI() {
        false
    }

    def start() {
        engine.init().join()
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thiefRequest = story.requests.find { it.template.persona.name == 'thief' } as PlayerRequest
        warrior = warriorRequest.template.createPlayer(Motivator.AI)
        thief = thiefRequest.template.createPlayer(Motivator.AI)
        engine.addToCast(warrior).join()
        engine.addToCast(thief).join()
        engine.start().join()
    }

    def "player explores to find room with no map"() {
        when:
        start()
        settle { engine.story.chronos.current }

        then:
        engine.story.getExtras(engine.story.world.findRoomById('dump').get())['thug']?.size() == 8
    }

    def "player goes to room with known map"() {
        given:
        engine.story.world.extras.each {
            it.knownRooms.addAll(engine.story.world.rooms)
        }
        when:
        start()
        settle { engine.story.chronos.current }

        then:
        engine.story.getExtras(engine.story.world.findRoomById('dump').get())['thug']?.size() == 8
        engine.story.chronos.current == 4
    }

    def "player explores to find room with partial map"() {
        given:
        engine.story.world.extras.each {
            it.knownRooms.addAll(engine.story.world.rooms.findAll { it.modelId in ['trailer_2', 'trailer_3' ]})
        }
        when:
        start()
        settle { engine.story.chronos.current }

        then:
        engine.story.getExtras(engine.story.world.findRoomById('dump').get())['thug']?.size() == 8
        engine.story.chronos.current == 6
    }
}

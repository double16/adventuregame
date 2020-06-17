package org.patdouble.adventuregame.storage.lua

import org.patdouble.adventuregame.model.Direction

import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World
import spock.lang.Specification

class TrailerParkStorageTest extends Specification {

    def "load"() {
        when: 'trailer-park.lua is loaded'
        InputStream is = getClass().getResourceAsStream('/worlds/trailer-park.lua')
        assert is
        World world = new WorldLuaStorage().load(is)

        then: 'World meta-data is present'
        world.name == LuaUniverseRegistry.TRAILER_PARK
        world.author == 'double16'
        world.description == 'A testing environment'

        and: 'personas are loaded'
        world.personas.size() == 3

        and: 'warrior persona'
        Persona warrior = world.personas.find { it.name == 'warrior' }
        warrior.name == 'warrior'
        warrior.health == 900
        warrior.wealth == 50

        and: 'players'
        world.players.size() == 3

        and: 'Shadowblow'
        PlayerTemplate shadowblow = world.players.find { it.nickName == 'Shadowblow' }
        shadowblow.nickName == 'Shadowblow'
        shadowblow.fullName == 'Shadowblow the Hammer'
        shadowblow.persona.name == 'warrior'
        shadowblow.quantity == 1..1
        shadowblow.room.modelId == 'entrance'

        and: 'Victor'
        PlayerTemplate victor = world.players.find { it.nickName == 'Victor' }
        victor.nickName == 'Victor'
        victor.fullName == 'Victor the Spider'
        victor.persona.name == 'thief'
        victor.quantity == 1..1
        victor.room.modelId == 'entrance'

        and: 'thug players'
        PlayerTemplate thugPlayer = world.players.find { it.persona.name == 'thug' }
        thugPlayer.nickName == null
        thugPlayer.fullName == null
        thugPlayer.persona.name == 'thug'
        thugPlayer.quantity == 0..10
        thugPlayer.room.modelId == 'dump'

        and: 'extras'
        world.extras.size() == 2

        and: 'thug extras in dump'
        PlayerTemplate thugExtra = world.extras.findAll { it.persona.name == 'thug' }[0]
        thugExtra.nickName == null
        thugExtra.fullName == 'Thug'
        thugExtra.persona.name == 'thug'
        thugExtra.quantity == 5..5
        thugExtra.room.modelId == 'dump'

        and: 'thug extras in entrance'
        PlayerTemplate thugExtra2 = world.extras.findAll { it.persona.name == 'thug' }[1]
        thugExtra2.nickName == null
        thugExtra2.fullName == 'Thug'
        thugExtra2.persona.name == 'thug'
        thugExtra2.quantity == 3..3
        thugExtra2.room.modelId == 'entrance'
        thugExtra2.goals.size() == 1
        with(thugExtra2.goals.first()) {
            !name
            required
            !theEnd
            description == 'Find the dump.'
            rules == [ 'player goes to room "dump"' ]
        }

        and: 'room count'
        world.rooms.size() == 7

        and: 'room entrance'
        Room rEntrance = world.rooms.find { it.modelId == 'entrance' }
        rEntrance.modelId == 'entrance'
        rEntrance.description == 'The entrance to the trailer park has a broken gate and a cluster of mail boxes.'
        rEntrance.getNeighbors().size() == 1
        Room rEntranceNorth = rEntrance.getNeighbors().get(Direction.NORTH.name().toLowerCase())
        rEntranceNorth.modelId == 'trailer_2'

        and: 'room trailer_1'
        Room rTrailer1 = world.rooms.find { it.modelId == 'trailer_1' }
        rTrailer1.modelId == 'trailer_1'
        rTrailer1.description == 'Trailer 1'
        rTrailer1.getNeighbors().size() == 1
        Room rTrailer2From1 = rTrailer1.getNeighbors().get(Direction.EAST.name().toLowerCase())
        rTrailer2From1.modelId == 'trailer_2'

        and: 'room trailer_2'
        Room rTrailer2 = world.rooms.find { it.modelId == 'trailer_2' }
        rTrailer2.modelId == 'trailer_2'
        rTrailer2.name == 'Trailer 2'
        rTrailer2.description == 'Trailer 2'
        rTrailer2.getNeighbors().size() == 3
        Room rEntranceFrom2 = rTrailer2.getNeighbors().get(Direction.SOUTH.name().toLowerCase())
        rEntranceFrom2.modelId == 'entrance'
        Room rTrailer1From2 = rTrailer2.getNeighbors().get(Direction.WEST.name().toLowerCase())
        rTrailer1From2.modelId == 'trailer_1'
        Room rTrailer3From2 = rTrailer2.getNeighbors().get(Direction.EAST.name().toLowerCase())
        rTrailer3From2.modelId == 'trailer_3'

        and: 'room trailer_3'
        Room rTrailer3 = world.rooms.find { it.modelId == 'trailer_3' }
        rTrailer3.modelId == 'trailer_3'
        rTrailer3.description == 'Trailer 3'
        rTrailer3.getNeighbors().size() == 2
        Room rTrailer2From3 = rTrailer3.getNeighbors().get(Direction.WEST.name().toLowerCase())
        rTrailer2From3.modelId == 'trailer_2'
        Room rTrailer4From3 = rTrailer3.getNeighbors().get(Direction.EAST.name().toLowerCase())
        rTrailer4From3.modelId == 'trailer_4'

        and: 'room trailer_4'
        Room rTrailer4 = world.rooms.find { it.modelId == 'trailer_4' }
        rTrailer4.modelId == 'trailer_4'
        rTrailer4.description == 'Trailer 4'
        rTrailer4.getNeighbors().size() == 2
        Room rTrailer3From4 = rTrailer4.getNeighbors().get(Direction.WEST.name().toLowerCase())
        rTrailer3From4.modelId == 'trailer_3'
        Room rDumpFrom4 = rTrailer4.getNeighbors().get(Direction.NORTH.name().toLowerCase())
        rDumpFrom4.modelId == 'dump'

        and: 'room dump'
        Room rDump = world.rooms.find { it.modelId == 'dump' }
        rDump.modelId == 'dump'
        rDump.description == 'Trash Dump'
        rDump.getNeighbors().size() == 2
        Room rTrailer4FromDump = rDump.getNeighbors().get(Direction.DOWN.name().toLowerCase())
        rTrailer4FromDump.modelId == 'trailer_4'
        Room rTrailer3FromDump = rDump.getNeighbors().get('dive')
        rTrailer3FromDump.modelId == 'trailer_3'

        and: 'Goals are present'
        world.goals.size() == 3
        with(world.goals.find { it.name == 'one' }) {
            !required
            !theEnd
            description == 'Any player reaches trailer 2.'
            rules == [ 'player enters room "trailer_2"' ]
        }
        with(world.goals.find { it.name == 'two' }) {
            !required
            theEnd
            description == 'Any player reaches trailer 4.'
            rules == [ 'player enters room "trailer_4"' ]
        }
        with(world.goals.find { it.name == 'three' }) {
            required
            !theEnd
            description == 'Unspecified'
            rules == []
        }
    }
}

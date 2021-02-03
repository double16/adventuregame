package org.patdouble.adventuregame.storage.lua

import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.World
import spock.lang.PendingFeature
import spock.lang.Specification

class WorldLuaStorageTest extends Specification {
    WorldLuaStorage luaStorage = new WorldLuaStorage()

    private World loadTheHobbit() {
        luaStorage.load(LuaUniverseRegistry.getResourceAsStream('/worlds/the-hobbit.lua'))
    }

    private World loadTrailerPark() {
        luaStorage.load(LuaUniverseRegistry.getResourceAsStream('/worlds/trailer-park.lua'))
    }

    def "load"() {
        when: 'The Hobbit is loaded'
        World world = loadTheHobbit()

        then: 'World meta-data is present'
        world.name == LuaUniverseRegistry.THE_HOBBIT
        world.author == 'double16'
        world.description == 'The Hobbit by J.R.R. Tolkien'

        and: 'personas are loaded'
        world.personas.size() == 6

        and: 'hobbit persona'
        Persona hobbit = world.personas.find { it.name == 'hobbit' }
        hobbit.name == 'hobbit'
        hobbit.health == 800
        hobbit.wealth == 300

        and: 'players'
        world.players.size() == 2

        and: 'regions'
        world.regions.size() == 11
        world.regions*.modelId.sort() == ['bag_end', 'bree', 'east', 'hobbiton', 'mordor', 'moria', 'north', 'old_forest', 'shire', 'south', 'west']
        with(world.findRegionById('bag_end').get()) {
            name == 'Bag End'
            description == 'Baggins\' Hobbit Hole'
            parent.modelId == 'hobbiton'
        }
        with(world.findRegionById('shire').get()) {
            name == 'The Shire'
            !description
        }
        with(world.findRegionById('mordor').get()) {
            name == 'Mordor'
            !description
        }

        and: 'rooms'
        world.rooms*.modelId.size() == 9
        with(world.findRoomById('bag_end_foyer').get()) {
            name == 'Bag End Foyer'
            description == 'Entrance to Bag End'
            region.modelId == 'bag_end'
        }

        and: 'Bilbo'
        with(world.players.find { it.nickName == 'Bilbo' }) {
            fullName == 'Bilbo Baggins'
            persona.name == 'hobbit'
            knownRooms*.modelId.sort() == ['bag_end_foyer', 'bag_end_garden', 'bag_end_kitchen', 'bag_end_parlor', 'hobbiton_east']
        }

        and: 'Gandalf'
        with (world.players.find { it.nickName == 'Gandalf' }) {
            fullName == 'Gandalf the Grey'
            persona.name == 'wizard'
            knownRooms*.modelId.sort() == ['bag_end_foyer', 'bag_end_garden', 'bag_end_kitchen', 'bag_end_parlor', 'blackgate', 'hobbiton_east']
        }

        and: 'orcs'
        with (world.extras.find { it.fullName == 'Orc' }) {
            persona.name == 'orc'
            knownRooms*.modelId.sort() == [ 'blackgate' ]
        }
    }

    def "hash code"() {
        expect:
        loadTheHobbit().hashCode() == loadTheHobbit().hashCode()
        loadTheHobbit().hashCode() != loadTrailerPark().hashCode()
    }

    def "equals"() {
        expect:
        loadTheHobbit() == loadTheHobbit()
        loadTheHobbit() != loadTrailerPark()
    }

    def "computeSecureHash"() {
        expect:
        loadTheHobbit().computeSecureHash() == loadTheHobbit().computeSecureHash()
        loadTrailerPark().computeSecureHash() == loadTrailerPark().computeSecureHash()
        loadTheHobbit().computeSecureHash() != loadTrailerPark().computeSecureHash()
    }

    def "computeSecureHash identifies changes"() {
        given:
        World w = loadTrailerPark()
        String originalHash = w.computeSecureHash()

        when: 'name is changed'
        World w2 = loadTrailerPark()
        w2.name += ' 2'
        then:
        w2.computeSecureHash() != originalHash

        when: 'description is changed'
        World w3 = loadTrailerPark()
        w3.description += ' 2'
        then:
        w3.computeSecureHash() != originalHash

        when: 'goal is changed'
        World w4 = loadTrailerPark()
        w4.goals[0].rules[0] = w4.goals[0].rules[0] + '2'
        then:
        w4.computeSecureHash() != originalHash
    }

    @PendingFeature
    def "rooms and regions are sorted"() {

    }
}

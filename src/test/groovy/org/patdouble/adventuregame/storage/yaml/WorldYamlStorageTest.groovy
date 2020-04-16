package org.patdouble.adventuregame.storage.yaml

import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.World
import spock.lang.Specification

class WorldYamlStorageTest extends Specification {
    YamlUniverseRegistry registry = new YamlUniverseRegistry()

    private World loadTheHobbit() {
        registry.worlds.find { it.name == YamlUniverseRegistry.THE_HOBBIT }
    }

    private World loadTrailerPark() {
        registry.worlds.find { it.name == YamlUniverseRegistry.TRAILER_PARK }
    }

    def "load"() {
        when: 'The Hobbit is loaded'
        World world = loadTheHobbit()

        then: 'World meta-data is present'
        world.name == YamlUniverseRegistry.THE_HOBBIT
        world.author == 'double16'
        world.description == 'The Hobbit by J.R.R. Tolkien'

        and: 'personas are loaded'
        world.personas.size() == 5

        and: 'warrior persona'
        Persona warrior = world.personas.find { it.name == 'warrior' }
        warrior.name == 'warrior'
        warrior.health == 100
        warrior.wealth == 50

        and: 'players'
        world.players.size() == 2

        and: 'Bilbo'
        PlayerTemplate bilbo = world.players.find { it.nickName == 'Bilbo' }
        bilbo.nickName == 'Bilbo'
        bilbo.fullName == 'Bilbo Baggins'
        bilbo.persona.name == 'hobbit'
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
}

package org.patdouble.adventuregame.storage.yaml

import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Player
import spock.lang.Specification

class WorldYamlStorageTest extends Specification {

    def "load"() {
        when: 'Middle Earth is loaded'
        InputStream is = getClass().getResourceAsStream('/worlds/middle-earth.yml')
        assert is
        World world = new WorldYamlStorage().load(is)

        then: 'World meta-data is present'
        world.name == 'Middle Earth'
        world.author == 'double16'
        world.description == 'The imaginary world of J.R.R. Tolkien'

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
}

package org.patdouble.adventuregame.model

import spock.lang.Specification

class UniverseRegistryTest extends Specification {
    UniverseRegistry registry

    def setup() {
        registry = new UniverseRegistry()
    }

    def "GetWorlds"() {
        when:
        List<World> worlds = registry.getWorlds()
        then:
        worlds.find { it.name == 'Trailer Park' }
        worlds.find { it.name == 'Middle Earth' }
    }
}

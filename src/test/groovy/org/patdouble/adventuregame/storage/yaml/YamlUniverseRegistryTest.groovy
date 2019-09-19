package org.patdouble.adventuregame.storage.yaml

import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import spock.lang.Specification

class YamlUniverseRegistryTest extends Specification {
    YamlUniverseRegistry registry

    def setup() {
        registry = new YamlUniverseRegistry()
    }

    def "GetWorlds"() {
        when:
        List<World> worlds = registry.getWorlds()
        then:
        worlds.find { it.name == YamlUniverseRegistry.TRAILER_PARK }
        worlds.find { it.name == YamlUniverseRegistry.MIDDLE_EARTH }
    }
}

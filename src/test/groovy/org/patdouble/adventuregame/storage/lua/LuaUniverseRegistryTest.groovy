package org.patdouble.adventuregame.storage.lua

import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import spock.lang.Specification

class LuaUniverseRegistryTest extends Specification {
    LuaUniverseRegistry registry

    def setup() {
        registry = new LuaUniverseRegistry()
    }

    def "GetWorlds"() {
        when:
        List<World> worlds = registry.getWorlds()
        then:
        worlds.find { it.name == LuaUniverseRegistry.TRAILER_PARK }
        worlds.find { it.name == LuaUniverseRegistry.THE_HOBBIT }
    }
}

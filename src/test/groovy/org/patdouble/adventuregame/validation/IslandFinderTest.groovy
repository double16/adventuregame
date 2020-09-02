package org.patdouble.adventuregame.validation

import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import spock.lang.Specification

class IslandFinderTest extends Specification {
    UniverseRegistry universeRegistry

    def setup() {
        universeRegistry = new LuaUniverseRegistry()
    }

    void "trailer park"() {
        when:
        IslandFinder finder = new IslandFinder(universeRegistry.worlds
               .find { it.name == LuaUniverseRegistry.TRAILER_PARK })
        Collection<Collection<Room>> islands = finder.computeIslands()

        then:
        islands.size() == 2
        islands.find { it.size() == 1 && it[0].modelId == 'nowhere' }
    }
}

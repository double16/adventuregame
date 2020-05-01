package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import spock.lang.Specification

class StoryTest extends Specification {
    LuaUniverseRegistry registry = new LuaUniverseRegistry()

    private Story newStory(String worldName) {
        new Story(registry.worlds.find { it.name == worldName })
    }

    def "Equals"() {
        expect:
        newStory(LuaUniverseRegistry.TRAILER_PARK) == newStory(LuaUniverseRegistry.TRAILER_PARK)
        newStory(LuaUniverseRegistry.TRAILER_PARK) != newStory(LuaUniverseRegistry.THE_HOBBIT)
    }

    def "Hashcode"() {
        expect:
        newStory(LuaUniverseRegistry.TRAILER_PARK).hashCode() == newStory(LuaUniverseRegistry.TRAILER_PARK).hashCode()
        newStory(LuaUniverseRegistry.TRAILER_PARK).hashCode() != newStory(LuaUniverseRegistry.THE_HOBBIT).hashCode()
    }
}

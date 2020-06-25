package org.patdouble.adventuregame.storage.lua

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.model.World

/**
 * Maintains a list of worlds.
 */
@CompileDynamic
class LuaUniverseRegistry implements UniverseRegistry {
    static final String TRAILER_PARK = 'Trailer Park'
    static final String THE_HOBBIT = 'The Hobbit'

    private final List<World> worlds

    LuaUniverseRegistry() {
        List<World> worlds = []

        WorldLuaStorage luaStorage = new WorldLuaStorage()
        worlds.addAll(['/worlds/the-hobbit.lua', '/worlds/lotr.lua', '/worlds/trailer-park.lua'].collect { String path ->
            InputStream is = LuaUniverseRegistry.getResourceAsStream(path)
            luaStorage.load(is)
        })

        this.worlds = worlds.asImmutable()
    }

    @Override
    List<World> getWorlds() {
        worlds
    }
}

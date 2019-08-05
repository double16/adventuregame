package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.storage.yaml.WorldYamlStorage

/**
 * Maintains a list of worlds.
 */
class UniverseRegistry {
    private List<World> worlds

    UniverseRegistry() {
        worlds = ['/worlds/middle-earth.yml', '/worlds/trailer-park.yml'].collect { String path ->
            InputStream is = UniverseRegistry.class.getResourceAsStream(path)
            new WorldYamlStorage().load(is)
        }.asImmutable()
    }

    List<World> getWorlds() {
        worlds
    }
}

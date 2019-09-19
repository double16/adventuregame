package org.patdouble.adventuregame.storage.yaml

import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.model.World

/**
 * Maintains a list of worlds.
 */
class YamlUniverseRegistry implements UniverseRegistry {
    static final String TRAILER_PARK = 'Trailer Park'
    static final String MIDDLE_EARTH = 'Middle Earth'

    private List<World> worlds

    YamlUniverseRegistry() {
        worlds = ['/worlds/middle-earth.yml', '/worlds/trailer-park.yml'].collect { String path ->
            InputStream is = YamlUniverseRegistry.class.getResourceAsStream(path)
            new WorldYamlStorage().load(is)
        }.asImmutable()
    }

    @Override
    List<World> getWorlds() {
        worlds
    }
}

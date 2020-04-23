package org.patdouble.adventuregame.storage.yaml

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.model.World

/**
 * Maintains a list of worlds.
 */
@CompileDynamic
class YamlUniverseRegistry implements UniverseRegistry {
    static final String TRAILER_PARK = 'Trailer Park'
    static final String THE_HOBBIT = 'The Hobbit'

    private final List<World> worlds

    YamlUniverseRegistry() {
        worlds = ['/worlds/the-hobbit.yml', '/worlds/trailer-park.yml'].collect { String path ->
            InputStream is = YamlUniverseRegistry.getResourceAsStream(path)
            new WorldYamlStorage().load(is)
        }.asImmutable()
    }

    @Override
    List<World> getWorlds() {
        worlds
    }
}

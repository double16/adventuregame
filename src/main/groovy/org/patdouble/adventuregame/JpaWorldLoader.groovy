package org.patdouble.adventuregame

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Populates the WorldRepository using JPA.
 */
@Configuration
@Slf4j
@CompileDynamic
class JpaWorldLoader {
    @Autowired
    WorldRepository worldRepository

    @Bean
    CommandLineRunner initDatabase() {
        LuaUniverseRegistry yamlUniverseRegistry = new LuaUniverseRegistry()

        return { args ->
            yamlUniverseRegistry.worlds.each { World world ->
                if (!worldRepository.findByName(world.name)) {
                    World saved = worldRepository.saveAndFlush(world)
                    JpaWorldLoader.log.info "Add world \"${saved.name}\" with ID ${saved.id}"
                }
            }
        }
    }
}

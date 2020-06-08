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

    /**
     * Ensure the World is persisted, only if there isn't an equivalent entity in the database.
     * @return persisted object, may be a different object than the argument
     */
    World save(World world) {
        String latestHash = world.computeSecureHash()
        List<World> active = worldRepository.findByNameAndActive(world.name, true)
        if (!active || active.any { it.hash != latestHash }) {
            active.each {
                it.active = false
                worldRepository.save(it)
            }

            world.edition = active.empty ? 1 : (active.max { it.edition }.edition + 1)
            world.active = true
            world.hash = latestHash
            World saved = worldRepository.saveAndFlush(world)
            JpaWorldLoader.log.info "Add world \"${saved.name}\" with ID ${saved.id}, edition ${world.edition}"
            return saved
        }

        return active.first()
    }

    @Bean
    @SuppressWarnings('Unused')
    CommandLineRunner initDatabase() {
        LuaUniverseRegistry yamlUniverseRegistry = new LuaUniverseRegistry()
        return { args ->
            yamlUniverseRegistry.worlds.each { World world ->
                save(world)
            }
        }
    }
}

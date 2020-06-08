package org.patdouble.adventuregame

import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional

@SpringBootTest
@AutoConfigureTestDatabase
@ContextConfiguration
@Transactional
@Unroll
class JpaWorldLoaderTest extends Specification {
    @Autowired
    WorldRepository worldRepository
    LuaUniverseRegistry universeRegistry = new LuaUniverseRegistry()

    def "world save #worldName"() {
        given:
        World luaWorld = universeRegistry.worlds.find { it.name == worldName }
        when:
        World jpaWorld = worldRepository.findByNameAndActive(worldName, true).first()

        // Ignore the following properties
        luaWorld.active = jpaWorld.active
        luaWorld.edition = jpaWorld.edition
        luaWorld.hash = jpaWorld.hash

        String diff = SpecHelper.unifiedDiffJson(luaWorld, jpaWorld)
        then:
        !diff

        where:
        worldName                         | _
        LuaUniverseRegistry.TRAILER_PARK  | _
        LuaUniverseRegistry.THE_HOBBIT    | _
    }

    def "new edition"() {
        given:
        JpaWorldLoader loader = new JpaWorldLoader()
        loader.worldRepository = worldRepository

        when: 'initial active world'
        World w1 = worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, true).first()
        then:
        w1.name == LuaUniverseRegistry.TRAILER_PARK
        w1.active
        w1.edition == 1

        when: 'save same world'
        World w1b = loader.save(universeRegistry.worlds.find { it.name == LuaUniverseRegistry.TRAILER_PARK })
        then:
        w1b.name == LuaUniverseRegistry.TRAILER_PARK
        w1b.active
        w1b.edition == 1
        and:
        worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, false).size() == 0

        when: 'save updated world'
        World w2 = universeRegistry.worlds.find { it.name == LuaUniverseRegistry.TRAILER_PARK }
        w2.description += ' 2'
        w2 = loader.save(w2)
        then:
        !w2.is(w1)
        w2.active
        w2.edition == 2
        and:
        worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, true).size() == 1
        worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, true).first().edition == 2
        and:
        worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, false).size() == 1
        worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, false).first().edition == 1
    }
}

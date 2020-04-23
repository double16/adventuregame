package org.patdouble.adventuregame

import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
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
    YamlUniverseRegistry universeRegistry = new YamlUniverseRegistry()

    def "world save #worldName"() {
        given:
        World yamlWorld = universeRegistry.worlds.find { it.name == worldName }
        when:
        World jpaWorld = worldRepository.findByName(worldName).first()
        then:
        jpaWorld == yamlWorld

        where:
        worldName                         | _
        YamlUniverseRegistry.TRAILER_PARK | _
        YamlUniverseRegistry.THE_HOBBIT | _
    }
}

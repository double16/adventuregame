package org.patdouble.adventuregame.storage.jpa

import org.patdouble.adventuregame.engine.DroolsConfiguration
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
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
class StoryRepositoryTest extends Specification {
    @Autowired
    WorldRepository worldRepository
    @Autowired
    StoryRepository storyRepository

    private Story newStory(String worldName, boolean start = false) {
        Story story = new Story(worldRepository.findByName(worldName).first())
        Engine engine = new Engine(story)
        engine.kContainer = new DroolsConfiguration().kieContainer()
        engine.init()
        if (start) {
            story.requests.clone().each { PlayerRequest req -> engine.addToCast(req.template.createPlayer(Motivator.HUMAN)) }
            engine.start()
        }
        story
    }

    def setup() {
        storyRepository.save(newStory(YamlUniverseRegistry.TRAILER_PARK, false))
        storyRepository.save(newStory(YamlUniverseRegistry.MIDDLE_EARTH, true))
    }

    def "save and load #worldName"() {
        given:
        Story s1 = newStory(worldName, start)
        when:
        Story s2 = storyRepository.findAll().find { it.world.name == s1.world.name } as Story
        then:
        s2
        !s1.is(s2)
        and:
        s1 == s2
        and:
        s2.requests

        where:
        worldName                         | start
        YamlUniverseRegistry.TRAILER_PARK | false
        YamlUniverseRegistry.MIDDLE_EARTH | true
    }
}

package org.patdouble.adventuregame.storage.jpa

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.patdouble.adventuregame.engine.DroolsConfiguration
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import org.slf4j.LoggerFactory
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
        engine.init().join()
        if (start) {
            story.requests.clone().each { PlayerRequest req -> engine.addToCast(req.template.createPlayer(Motivator.HUMAN)) }
            engine.start().join()
        }
        engine.close()
        story
    }

    def setup() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO)
        ((Logger) LoggerFactory.getLogger('org.hibernate.SQL')).setLevel(Level.DEBUG)

        storyRepository.saveAndFlush(newStory(YamlUniverseRegistry.TRAILER_PARK, false))
        storyRepository.saveAndFlush(newStory(YamlUniverseRegistry.THE_HOBBIT, true))
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
        and:
        s2.id
        and:
        if (start) {
            s1.cast.size() > 0
            s2.cast.count { it.id } > 0
        }

        where:
        worldName                         | start
        YamlUniverseRegistry.TRAILER_PARK | false
        YamlUniverseRegistry.THE_HOBBIT | true
    }

    def "cast added later"() {
        given:
        Story s = newStory(YamlUniverseRegistry.TRAILER_PARK, false)

        when:
        Story saveResult = storyRepository.saveAndFlush(s)
        then:
        s.id
        saveResult == s
        s.cast.count { it.id } == 0

        when:
        s.requests.each { PlayerRequest req ->
            s.cast << req.template.createPlayer(Motivator.HUMAN)
        }
        s.requests.clear()
        storyRepository.saveAndFlush(s)
        then:
        s.cast.count { it.id } > 0
    }

    def "timestamps"() {
        given:
        Story s = newStory(YamlUniverseRegistry.TRAILER_PARK, true)

        when:
        storyRepository.saveAndFlush(s)
        then:
        s.created
        s.modified

        when:
        def created1 = s.created
        def modified1 = s.modified
        s.chronos.next()
        storyRepository.saveAndFlush(s)
        then:
        modified1 != s.modified
        created1 == s.created
    }
}

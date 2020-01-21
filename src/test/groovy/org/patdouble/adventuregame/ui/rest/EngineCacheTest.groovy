package org.patdouble.adventuregame.ui.rest

import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistryTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional

@SpringBootTest
@AutoConfigureTestDatabase
@ContextConfiguration
@Transactional
@Unroll
class EngineCacheTest extends Specification {
    @Autowired
    WorldRepository worldRepository
    @Autowired
    StoryRepository storyRepository
    @Autowired
    EngineCache cache

    def setup() {

    }

    def cleanup() {

    }

    def "get new story"() {
        given:
        Story story = new Story(worldRepository.findByName(YamlUniverseRegistry.TRAILER_PARK).first())
        storyRepository.save(story)

        when:
        Engine engine = cache.get(story.id)

        then:
        engine.autoLifecycle
        engine.story.id == story.id
        engine.story.chronos.current == 0
        engine.story.requests.size() == 12
        engine.story.goals.size() == 3
    }

    def "get init story"() {
        given:
        Story story = new Story(worldRepository.findByName(YamlUniverseRegistry.TRAILER_PARK).first())
        Engine engine1 = new Engine(story)
        engine1.init()
        engine1.close()
        storyRepository.save(story)

        when:
        Engine engine = cache.get(story.id)

        then:
        engine.autoLifecycle
        engine.story.id == story.id
        engine.story.chronos.current == 0
        engine.story.requests.size() == 12
        engine.story.goals.size() == 3
    }

    def "get started story"() {
        given:
        Story story = new Story(worldRepository.findByName(YamlUniverseRegistry.TRAILER_PARK).first())
        Engine engine1 = new Engine(story)
        engine1.chronosLimit = 5
        engine1.init()
        story.requests.findAll { it instanceof PlayerRequest }.each { PlayerRequest request ->
            engine1.addToCast(request.template.createPlayer(Motivator.HUMAN))
        }
        engine1.start()
        engine1.close()
        storyRepository.save(story)

        when:
        Engine engine = cache.get(story.id)

        then:
        engine.autoLifecycle
        engine.story.id == story.id
        engine.story.chronos.current == 1
        engine.story.requests.size() == 12
        engine.story.goals.size() == 3
    }

    def "get ended story"() {
        given:
        Story story = new Story(worldRepository.findByName(YamlUniverseRegistry.TRAILER_PARK).first())
        Engine engine1 = new Engine(story)
        engine1.chronosLimit = 5
        engine1.init()
        story.requests.findAll { it instanceof PlayerRequest }.each { PlayerRequest request ->
            engine1.addToCast(request.template.createPlayer(Motivator.HUMAN))
        }
        engine1.start()
        engine1.end()
        engine1.close()
        storyRepository.save(story)

        when:
        cache.get(story.id)

        then:
        thrown(ResponseStatusException)
    }

    def "clear"() {

    }

    def "expire"() {

    }

    def "regular save"() {

    }
}

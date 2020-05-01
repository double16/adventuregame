package org.patdouble.adventuregame.ui.rest

import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

    def "get new story"() {
        given:
        Story story = new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first())
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
        Story story = new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first())
        Engine engine1 = new Engine(story)
        engine1.init().join()
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
        Story story = new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first())
        Engine engine1 = new Engine(story)
        engine1.chronosLimit = 5
        engine1.init().join()
        story.requests.findAll { it instanceof PlayerRequest }.each { PlayerRequest request ->
            engine1.addToCast(request.template.createPlayer(Motivator.HUMAN)).join()
        }
        engine1.start().join()
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
        Story story = new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first())
        Engine engine1 = new Engine(story)
        engine1.chronosLimit = 5
        engine1.init().join()
        story.requests.findAll { it instanceof PlayerRequest }.each { PlayerRequest request ->
            engine1.addToCast(request.template.createPlayer(Motivator.HUMAN)).join()
        }
        engine1.start().join()
        engine1.end().join()
        engine1.close()
        storyRepository.save(story)

        when:
        cache.get(story.id)

        then:
        thrown(ResponseStatusException)
    }

    def "clear"() {
        given:
        List<Engine> engines = []
        engines << cache.get(storyRepository.save(new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first())).id)
        engines << cache.get(storyRepository.save(new Story(worldRepository.findByName(LuaUniverseRegistry.THE_HOBBIT).first())).id)
        List<LocalDateTime> modified = engines.collect { it.story.modified }

        when:
        engines.each { Engine e ->
            e.addToCast(e.story.requests.find { it instanceof PlayerRequest }.template.createPlayer(Motivator.HUMAN)).join()
        }
        cache.clear()

        then:
        engines[0].story.modified > modified[0]
        engines[1].story.modified > modified[1]
        engines.every { it.isClosed() }
    }

    def "expire"() {
        given:
        cache.ttl = Duration.of(10000, ChronoUnit.MILLIS)
        List<Story> stories = []
        stories << storyRepository.saveAndFlush(new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first()))
        stories << storyRepository.saveAndFlush(new Story(worldRepository.findByName(LuaUniverseRegistry.THE_HOBBIT).first()))
        List<LocalDateTime> modified = stories.collect { it.modified }
        when:
        cache.get(stories[1].id).with { Engine e ->
            e.addToCast(e.story.requests.find { it instanceof PlayerRequest }.template.createPlayer(Motivator.HUMAN)).join()
        }
        cache.expire()
        storyRepository.flush()
        then:
        cache.size() == 1
        and: 'expired story was saved'
        storyRepository.findById(stories[1].id).get().modified > modified[1]
    }

    def "remove ended"() {
        given:
        cache.ttl = Duration.of(100000, ChronoUnit.MILLIS)
        List<Story> stories = []
        stories << storyRepository.saveAndFlush(new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first()))
        stories << storyRepository.saveAndFlush(new Story(worldRepository.findByName(LuaUniverseRegistry.THE_HOBBIT).first()))
        List<LocalDateTime> modified = stories.collect { it.modified }
        when:
        cache.get(stories[1].id).with { Engine e ->
            e.story.ended = true
            storyRepository.saveAndFlush(e.story)
        }
        cache.expire()
        then:
        cache.size() == 1
        and: 'ended story was saved'
        storyRepository.findById(stories[1].id).get().modified > modified[1]
    }

    def "save at intervals"() {
        given:
        cache.clear()
        List<Engine> engines = []
        engines << cache.get(storyRepository.save(new Story(worldRepository.findByName(LuaUniverseRegistry.TRAILER_PARK).first())).id)
        engines << cache.get(storyRepository.save(new Story(worldRepository.findByName(LuaUniverseRegistry.THE_HOBBIT).first())).id)
        List<LocalDateTime> modified = engines.collect { it.story.modified }

        when:
        engines.each { Engine e ->
            e.addToCast(e.story.requests.find { it instanceof PlayerRequest }.template.createPlayer(Motivator.HUMAN)).join()
        }
        cache.sweep()
        cache.storyRepository.flush()

        then:
        engines[0].story.modified > modified[0]
        engines[1].story.modified > modified[1]
        engines.every { !it.isClosed() }
    }
}

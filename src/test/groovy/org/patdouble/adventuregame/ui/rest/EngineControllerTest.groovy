package org.patdouble.adventuregame.ui.rest

import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.engine.RecordingSimpMessageTemplate
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.ErrorMessage
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll
import static org.patdouble.adventuregame.SpecHelper.wait

import javax.transaction.Transactional

@SpringBootTest
@AutoConfigureTestDatabase
@ContextConfiguration
@Transactional
@Unroll
class EngineControllerTest extends Specification {

    @Autowired
    EngineController controller
    RecordingSimpMessageTemplate simpMessagingTemplate
    String storyId

    void setup() {
        controller.engineCache.autoLifecycle = false
        simpMessagingTemplate = new RecordingSimpMessageTemplate()
        controller.simpMessagingTemplate = simpMessagingTemplate
        controller.engineCache.simpMessagingTemplate = simpMessagingTemplate
        CreateStoryRequest request = new CreateStoryRequest(worldName: YamlUniverseRegistry.TRAILER_PARK)
        CreateStoryResponse response = controller.createStory(request)
        storyId = response.storyUri.split('/').last()
    }

    void cleanup() {
        controller.engineCache.clear()
        controller.engineCache.autoLifecycle = true
    }

    def "CreateStory by name"() {
        given:
        CreateStoryRequest request = new CreateStoryRequest(worldName: YamlUniverseRegistry.TRAILER_PARK)

        when:
        CreateStoryResponse response = controller.createStory(request)

        then:
        response.storyUri =~ '/play/[A-Fa-f0-9-]+'
    }

    def "CreateStory by id"() {
        given:
        String id = controller.worldRepository.findByName(YamlUniverseRegistry.TRAILER_PARK).first().id.toString()
        System.out.println "worldId = ${id}"
        CreateStoryRequest request = new CreateStoryRequest(worldId: id)

        when:
        CreateStoryResponse response = controller.createStory(request)

        then:
        response.storyUri =~ '/play/[A-Fa-f0-9-]+'
    }

    def "CreateStory by URI"() {
        given:
        String id = "http://localhost:8080/api/worlds/"+controller.worldRepository.findByName(YamlUniverseRegistry.TRAILER_PARK).first().id.toString()
        System.out.println "worldId = ${id}"
        CreateStoryRequest request = new CreateStoryRequest(worldId: id)

        when:
        CreateStoryResponse response = controller.createStory(request)

        then:
        response.storyUri =~ '/play/[A-Fa-f0-9-]+'
    }

    def "AddToCast"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        String warriorTemplateId = engine.story.requests
                .find { (it instanceof PlayerRequest) && it.template.persona.name == PersonaMocks.WARRIOR.name }
                .template.id as String

        when:
        AddToCastResponse response = controller.addToCast(new AddToCastRequest(
                storyId: storyId,
                playerTemplateId: warriorTemplateId,
                motivator: 'human',
                fullName: 'First Last',
                nickName: 'Firstly'))
        then:
        response.playerUri =~ '/play/[A-Fa-f0-9-]+/[A-Fa-f0-9-]+'
        and:
        wait {
            simpMessagingTemplate.messages.find {
                it.first == "/topic/story.${storyId}" as String &&
                it.second.payload instanceof RequestSatisfied &&
                it.second.payload.request.template.id.toString() == warriorTemplateId &&
                it.second.headers['type'] == 'RequestSatisfied'
            }
        }
    }

    def "Ignore required"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        String warriorTemplateId = engine.story.requests
                .find { (it instanceof PlayerRequest) && it.template.persona.name == PersonaMocks.WARRIOR.name }
                .template.id as String

        when:
        controller.ignore(new IgnoreCastRequest(
                storyId: storyId,
                playerTemplateId: warriorTemplateId))

        then:
        wait {
            assert engine.story.requests.size() == 12
            assert engine.story.requests.find { (it instanceof PlayerRequest) && it.template.id.toString() == warriorTemplateId }
        }
        and:
        !simpMessagingTemplate.messages.find {
            it.first == "/topic/story.${storyId}" as String &&
                    it.second.payload instanceof RequestSatisfied &&
                    it.second.payload.request.template.id.toString() == warriorTemplateId &&
                    it.second.headers['type'] == 'RequestSatisfied'
        }
    }

    def "Ignore optional"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        String thugTemplateId = engine.story.requests
                .find { (it instanceof PlayerRequest) && it.template.persona.name == 'thug' }
                .template.id as String

        when:
        controller.ignore(new IgnoreCastRequest(
                storyId: storyId,
                playerTemplateId: thugTemplateId))

        then:
        engine.story.requests.size() == 2
        !engine.story.requests.find { (it instanceof PlayerRequest) && it.template.id.toString() == thugTemplateId }
        and:
        wait {
            simpMessagingTemplate.messages.find {
                it.first == "/topic/story.${storyId}" as String &&
                        it.second.payload instanceof RequestSatisfied &&
                        it.second.payload.request.template.id.toString() == thugTemplateId &&
                        it.second.headers['type'] == 'RequestSatisfied'
            }
        }
    }

    def "Start"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        engine.story.requests.findAll { it instanceof PlayerRequest }.each {
            controller.addToCast(new AddToCastRequest(
                    storyId: storyId,
                    playerTemplateId: it.template.id as String,
                    motivator: 'human'))
        }
        when:
        controller.start(new StartRequest(storyId: storyId))

        then:
        notThrown(Exception)
        and:
        wait { assert engine.story.chronos.current > 0 }
        and:
        wait {
            simpMessagingTemplate.messages.find {
                it.first == "/topic/story.${storyId}" as String &&
                        it.second.payload instanceof ChronosChanged &&
                        it.second.headers['type'] == 'ChronosChanged'
            }
        }
    }

    def "Action"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        engine.story.requests.findAll { it instanceof PlayerRequest }.each {
            controller.addToCast(new AddToCastRequest(
                    storyId: storyId,
                    playerTemplateId: it.template.id as String,
                    motivator: 'human'))
        }
        controller.start(new StartRequest(storyId: storyId))
        Player warrior = engine.story.cast.find { it.persona.name == PersonaMocks.WARRIOR.name }

        when:
        controller.action(new ActionRequest(
                storyId: storyId,
                playerId: warrior.id as String,
                statement: 'go north'
        ))

        then:
        wait {
            assert warrior.room.modelId == 'trailer_2'
        }
        and:
        wait {
            simpMessagingTemplate.messages.find {
                it.first == "/topic/story.${storyId}" as String &&
                        it.second.payload instanceof PlayerChanged &&
                        it.second.payload.player.nickName == 'Shadowblow' &&
                        it.second.payload.chronos == 1 &&
                        it.second.headers['type'] == 'PlayerChanged'
            }
        }
    }

    def "handleException"() {
        when:
        ErrorMessage err1 = controller.handleException(new ResponseStatusException(HttpStatus.NOT_FOUND, 'Story not found'))
        then:
        err1.httpCode == 404
        err1.message == 'Story not found'

        when:
        ErrorMessage err2 = controller.handleException(new ResponseStatusException(HttpStatus.CONFLICT))
        then:
        err2.httpCode == 409
        err2.message == 'Conflict'

        when:
        ErrorMessage err3 = controller.handleException(new IllegalStateException('Something is not right'))
        then:
        err3.httpCode == 500
        err3.message == 'Something is not right'
    }
}

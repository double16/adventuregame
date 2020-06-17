package org.patdouble.adventuregame.ui.rest

import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.engine.RecordingSimpMessageTemplate
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.ErrorMessage
import org.patdouble.adventuregame.flow.GoalFulfilled
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.flow.StoryEnded
import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.lua.LuaUniverseRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.server.ResponseStatusException
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll
import static org.patdouble.adventuregame.SpecHelper.settle

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
        CreateStoryRequest request = new CreateStoryRequest(worldName: LuaUniverseRegistry.TRAILER_PARK)
        CreateStoryResponse response = controller.createStory(request)
        storyId = response.storyUri.split('/').last()
    }

    void cleanup() {
        controller.engineCache.clear()
        controller.engineCache.autoLifecycle = true
    }

    protected void waitForStompMessages() {
        settle { simpMessagingTemplate.messages.size() }
    }

    def "CreateStory by name"() {
        given:
        CreateStoryRequest request = new CreateStoryRequest(worldName: LuaUniverseRegistry.TRAILER_PARK)

        when:
        CreateStoryResponse response = controller.createStory(request)

        then:
        response.storyUri =~ '/play/[A-Fa-f0-9-]+'
    }

    def "CreateStory by id"() {
        given:
        String id = controller.worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, true).first().id.toString()
        System.out.println "worldId = ${id}"
        CreateStoryRequest request = new CreateStoryRequest(worldId: id)

        when:
        CreateStoryResponse response = controller.createStory(request)

        then:
        response.storyUri =~ '/play/[A-Fa-f0-9-]+'
    }

    def "CreateStory by URI"() {
        given:
        String id = "http://localhost:8080/api/worlds/"+controller.worldRepository.findByNameAndActive(LuaUniverseRegistry.TRAILER_PARK, true).first().id.toString()
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
        waitForStompMessages()
        then:
        response.playerUri =~ '/play/[A-Fa-f0-9-]+/[A-Fa-f0-9-]+'
        and:
        simpMessagingTemplate.messages.find {
            it.first == "/topic/story.${storyId}" as String &&
            it.second.payload instanceof RequestSatisfied &&
            it.second.payload.request.template.id.toString() == warriorTemplateId &&
            it.second.headers['type'] == 'RequestSatisfied'
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
        settle { engine.story.requests.size() }
        then:
        engine.story.requests.size() == 12
        engine.story.requests.find { (it instanceof PlayerRequest) && it.template.id.toString() == warriorTemplateId }
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
        waitForStompMessages()

        then:
        engine.story.requests.size() == 2
        !engine.story.requests.find { (it instanceof PlayerRequest) && it.template.id.toString() == thugTemplateId }
        and:
        simpMessagingTemplate.messages.find {
            it.first == "/topic/story.${storyId}" as String &&
                    it.second.payload instanceof RequestSatisfied &&
                    it.second.payload.request.template.id.toString() == thugTemplateId &&
                    it.second.headers['type'] == 'RequestSatisfied'
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
        waitForStompMessages()

        then:
        notThrown(Exception)
        and:
        engine.story.chronos.current > 0
        and:
        simpMessagingTemplate.messages.find {
            it.first == "/topic/story.${storyId}" as String &&
                    it.second.payload instanceof ChronosChanged &&
                    it.second.headers['type'] == 'ChronosChanged'
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
                statement: 'go north',
                waitForComplete: true
        ))
        waitForStompMessages()

        then:
        warrior.room.modelId == 'trailer_2'
        and:
        simpMessagingTemplate.messages.find {
            it.first == "/topic/story.${storyId}" as String &&
                    it.second.payload instanceof PlayerChanged &&
                    it.second.payload.player.nickName == 'Shadowblow' &&
                    it.second.payload.chronos == 1 &&
                    it.second.headers['type'] == 'PlayerChanged'
        }
    }

    def "Action with invalid statement"() {
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
                statement: 'go nowhere',
                waitForComplete: true
        ))
        waitForStompMessages()

        then:
        def e = thrown(ResponseStatusException)
        e.status == HttpStatus.NOT_ACCEPTABLE
    }

    def "State"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        engine.story.requests.findAll { it instanceof PlayerRequest }.each {
            controller.addToCast(new AddToCastRequest(
                    storyId: storyId,
                    playerTemplateId: it.template.id as String,
                    motivator: 'human'))
        }
        engine.story.goals.find { it.goal.name == 'one' }.fulfilled = true
        controller.start(new StartRequest(storyId: storyId, waitForComplete: true))
        when:
        List<StoryMessage> messages = controller.state(storyId)

        then:
        messages.size() == 34
        messages.count { it instanceof RequestCreated } == 12
        messages.count { it instanceof PlayerChanged } == 20
        messages.count { it instanceof ChronosChanged } == 1
        messages.count { it instanceof GoalFulfilled } == 1
    }

    def "State for ended story"() {
        given:
        Engine engine = controller.engineCache.get(UUID.fromString(storyId))
        engine.story.requests.findAll { it instanceof PlayerRequest }.each {
            controller.addToCast(new AddToCastRequest(
                    storyId: storyId,
                    playerTemplateId: it.template.id as String,
                    motivator: 'human'))
        }
        engine.story.goals.find { it.goal.name == 'one' }.fulfilled = true
        controller.start(new StartRequest(storyId: storyId, waitForComplete: true))
        engine.end().join()
        controller.engineCache.expire().join()

        when:
        List<StoryMessage> messages = controller.state(storyId)

        then:
        messages.size() == 1
        messages.count { it instanceof StoryEnded } == 1
    }

    def "handleException #code"() {
        given:
        MockHttpServletResponse response = new MockHttpServletResponse()

        when:
        ErrorMessage err1 = controller.handleException(exception)
        then:
        err1.httpCode == code
        err1.message == message

        when:
        ErrorMessage err2 = controller.handleExceptionHttp(exception, response)
        then:
        err2.httpCode == code
        err2.message == message
        and:
        response.status == code
        response.errorMessage == message

        where:
        code | message                  | exception
        404  | 'Story not found'        | new ResponseStatusException(HttpStatus.NOT_FOUND, 'Story not found')
        409  | 'Conflict'               | new ResponseStatusException(HttpStatus.CONFLICT)
        500  | 'Something is not right' | new IllegalStateException('Something is not right')
        406  | 'Invalid UUID'           | new IllegalArgumentException('Invalid UUID')
    }

    @PendingFeature
    def "respond with busy code when memory usage is high"() {
        expect:
        false
    }
}

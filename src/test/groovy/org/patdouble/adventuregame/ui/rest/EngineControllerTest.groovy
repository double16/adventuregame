package org.patdouble.adventuregame.ui.rest

import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.test.context.ContextConfiguration
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
    SimpMessagingTemplate simpMessagingTemplate
    String storyId

    void setup() {
        controller.engineCache.autoLifecycle = false
        simpMessagingTemplate = Mock()
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

    def "CreateStory"() {
        given:
        CreateStoryRequest request = new CreateStoryRequest(worldName: YamlUniverseRegistry.TRAILER_PARK)

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
            1 * simpMessagingTemplate.convertAndSend(
                    "/topic/story/${storyId}",
                    { it instanceof RequestSatisfied && it.request.template.id.toString() == warriorTemplateId },
                    ['type':'RequestSatisfied']
            )
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
        thrown(IllegalArgumentException)
        engine.story.requests.size() == 12
        engine.story.requests.find { (it instanceof PlayerRequest) && it.template.id.toString() == warriorTemplateId }
        and:
        0 * simpMessagingTemplate.convertAndSend(_, _)
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
        0 * simpMessagingTemplate.convertAndSend(_, _)
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
            1 * simpMessagingTemplate.convertAndSend(
                    "/topic/story/${storyId}",
                    { it instanceof ChronosChanged },
                    ['type':'ChronosChanged'])
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
            assert warrior.room.id == 'trailer_2'
        }
        and:
        wait {
            1 * simpMessagingTemplate.convertAndSend(
                    "/topic/story/${storyId}",
                    { it instanceof PlayerChanged && it.player.nickName == 'Shadowblow' && it.chronos == 1 },
                    ['type':'PlayerChanged'])
        }
    }
}

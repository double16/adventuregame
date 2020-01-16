package org.patdouble.adventuregame.ui.rest

import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.PersonaMocks
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
class EngineControllerTest extends Specification {

    @Autowired
    EngineController controller
    String storyId

    void setup() {
        CreateStoryRequest request = new CreateStoryRequest(worldName: YamlUniverseRegistry.TRAILER_PARK)
        CreateStoryResponse response = controller.createStory(request)
        storyId = response.storyUri.split('/').last()
    }

    void cleanup() {
    }

    def "CreateStory"() {
        given:
        CreateStoryRequest request = new CreateStoryRequest(worldName: YamlUniverseRegistry.TRAILER_PARK)

        when:
        CreateStoryResponse response = controller.createStory(request)

        then:
        response.storyUri =~ '/engine/play/[A-Za-z0-9-]+'
    }

    def "Action"() {
        // TODO
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
        response.playerUri =~ '/engine/play/[A-Za-z0-9-]+/[A-Za-z0-9-]+'
    }

    def "Ignore"() {
        // TODO
    }

    def "Start"() {
        // TODO
    }
}

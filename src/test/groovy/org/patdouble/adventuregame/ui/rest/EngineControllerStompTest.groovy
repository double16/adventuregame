package org.patdouble.adventuregame.ui.rest

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator
import org.springframework.web.socket.messaging.WebSocketStompClient
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional
import java.lang.reflect.Type
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@ContextConfiguration
@Transactional
@Unroll
@Slf4j
class EngineControllerStompTest extends Specification {
    static final int POLL_TIMEOUT = 15

    @Autowired
    EngineController controller
    @LocalServerPort
    int port
    String storyId
    WebSocketStompClient stompClient
    CompletableFuture<Boolean> subscribed = new CompletableFuture<>()
    BlockingQueue<Tuple2<StompHeaders, Object>> messages = new ArrayBlockingQueue<>(100)

    void setup() {
        ((Logger) LoggerFactory.getLogger(LoggingWebSocketHandlerDecorator.class.name)).setLevel(Level.TRACE)

        CreateStoryRequest request = new CreateStoryRequest(worldName: YamlUniverseRegistry.TRAILER_PARK)
        CreateStoryResponse response = controller.createStory(request)
        storyId = response.storyUri.split('/').last()

        WebSocketClient webSocketClient = new StandardWebSocketClient()
        stompClient = new WebSocketStompClient(webSocketClient)
        stompClient.setMessageConverter(new MappingJackson2MessageConverter())
        stompClient.connect("ws://localhost:${port}/socket", new StompSessionHandlerAdapter() {
            @Override
            void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                log.info "STOMP session ${session.sessionId} subscribing to story ${storyId}"
                session.subscribe("/topic/story.${storyId}", new StompFrameHandler() {
                    @Override
                    Type getPayloadType(StompHeaders headers) {
                        String type = headers.getFirst('type')
                        if (!type) {
                            return null
                        }
                        Class.forName("org.patdouble.adventuregame.flow.${type}")
                    }

                    @Override
                    void handleFrame(StompHeaders headers, @Nullable Object payload) {
                        log.info "STOMP frame ${payload}"
                        messages << new Tuple2(headers, payload)
                    }
                })
                subscribed.complete(true)
            }
        })
        if (!subscribed.get(5, TimeUnit.SECONDS)) {
            throw new RuntimeException('STOMP subscription timeout')
        }
    }

    void cleanup() {
        controller.engineCache.clear()
    }

    protected Collection<Tuple2<StompHeaders, Object>> pollMessages(int timeout, TimeUnit unit) {
        LinkedList<Tuple2<StompHeaders, Object>> result = []
        def m
        while ((m = messages.poll(timeout, unit)) != null) {
            result << m
        }
        result
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
        pollMessages(POLL_TIMEOUT, TimeUnit.SECONDS)
                .collect { it.second }
                .find { it instanceof RequestSatisfied && it.request.template.id.toString() == warriorTemplateId }
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
        !messages.poll(POLL_TIMEOUT, TimeUnit.SECONDS)
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
        pollMessages(POLL_TIMEOUT, TimeUnit.SECONDS)
                .collect { it.second }
                .find { it instanceof RequestSatisfied && it.request.template.id.toString() == thugTemplateId }
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
        engine.story.chronos.current > 0
        and:
        pollMessages(POLL_TIMEOUT, TimeUnit.SECONDS)
                .collect { it.second }
                .find { it instanceof ChronosChanged }
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
        warrior.room.modelId == 'trailer_2'
        and:
        pollMessages(POLL_TIMEOUT, TimeUnit.SECONDS)
                .collect { it.second }
                .find { it instanceof PlayerChanged && it.player.nickName == 'Shadowblow' && it.chronos == 1 }
    }
}

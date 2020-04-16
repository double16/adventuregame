package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * REST controller for the lifecycle of an Engine.
 */
@RestController
@CompileDynamic
@RequestMapping('/api/engine')
@MessageMapping//('/engine')
@SuppressWarnings('Instanceof')
class EngineController {
    @Autowired
    EngineCache engineCache
    @Autowired
    WorldRepository worldRepository
    @Autowired
    SimpMessageSendingOperations simpMessagingTemplate

    @MessageMapping('/createstory')
    @SendTo('/topic/storyurl')
    @RequestMapping(
            method = RequestMethod.POST,
            path = 'createstory',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    CreateStoryResponse createStory(@Payload @RequestBody CreateStoryRequest request) {
        World world = null
        if (request.worldId) {
            world = worldRepository.findById(UUID.fromString(request.worldId.split('/').last())).get()
        }
        if (request.worldName && !world) {
            world = worldRepository.findByName(request.worldName).first()
        }
        if (!world) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World ${request} not found")
        }
        Engine engine = engineCache.create(world)
        new CreateStoryResponse(storyUri: "/play/${engine.story.id}")
    }

    @SubscribeMapping('/story.{storyId}')
    List<StoryMessage> subscribe(@DestinationVariable('storyId') String storyId) {
        Engine engine = requireEngine(storyId)
        engine.story.requests.collect { new RequestCreated(it) } + engine.story.cast.collect { new PlayerNotification(it) }
    }

    @MessageMapping('/action')
    @RequestMapping(
            method = RequestMethod.POST,
            path = 'action',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    void action(@Payload @RequestBody ActionRequest actionRequest) {
        Engine engine = requireEngine(actionRequest.storyId)
        Player player = engine.story.cast.find { it.id == UUID.fromString(actionRequest.playerId) }
        if (!player) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player ${actionRequest.playerId} not found")
        }
        engine.action(player, actionRequest.statement)
    }

    @MessageMapping('/addtocast')
    @RequestMapping(
            method = RequestMethod.POST,
            path = 'addtocast',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    AddToCastResponse addToCast(@Payload @RequestBody AddToCastRequest request) {
        Engine engine = requireEngine(request.storyId)
        PlayerRequest playerRequest = (PlayerRequest) engine.story.requests
                .find { (it instanceof PlayerRequest) && it.template.id == UUID.fromString(request.playerTemplateId) }
        if (!playerRequest) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Player template ${request.playerTemplateId} not found")
        }
        Player player = playerRequest.template.createPlayer(Motivator.valueOf(request.motivator.toUpperCase()))
        if (request.fullName) {
            player.fullName = request.fullName
        }
        if (request.nickName) {
            player.nickName = request.nickName
        }
        engine.addToCast(player)
        Player savedPlayer = engine.story.cast.find { it == player }
        Objects.requireNonNull(savedPlayer?.id)
        new AddToCastResponse(playerUri: "/play/${engine.story.id}/${savedPlayer.id}")
    }

    @MessageMapping('/ignorecast')
    @RequestMapping(
            method = RequestMethod.POST,
            path = 'ignorecast',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    void ignore(@Payload @RequestBody IgnoreCastRequest request) {
        Engine engine = requireEngine(request.storyId)
        engine.story.requests
            .findAll { (it instanceof PlayerRequest) && it.template.id == UUID.fromString(request.playerTemplateId) }
            .each {
                engine.ignore(it as PlayerRequest)
            }
    }

    @MessageMapping('/start')
    @RequestMapping(
            method = RequestMethod.POST,
            path = 'start',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    void start(@Payload @RequestBody StartRequest request) {
        Engine engine = requireEngine(request.storyId)
        engine.start(Motivator.AI)
    }

    private Engine requireEngine(storyId) throws ResponseStatusException {
        Engine engine = engineCache.get(UUID.fromString(storyId as String))
        if (!engine) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story ${storyId} not found")
        }
        engine
    }
}

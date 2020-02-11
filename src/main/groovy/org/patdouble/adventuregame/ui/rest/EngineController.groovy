package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
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
@MessageMapping('/engine')
@SuppressWarnings('Instanceof')
class EngineController {
    @Autowired
    EngineCache engineCache
    @Autowired
    WorldRepository worldRepository
    @Autowired
    StoryRepository storyRepository
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate

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
            world = worldRepository.findById(UUID.fromString(request.worldId)).get()
        }
        if (request.worldName && !world) {
            world = worldRepository.findByName(request.worldName).first()
        }
        if (!world) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World ${request} not found")
        }
        Story story = new Story(world)
        Engine engine = new Engine(story)
        engine.autoLifecycle = true
        engine.init()
        storyRepository.save(story)
        Objects.requireNonNull(story.id)
        new CreateStoryResponse(storyUri: "/play/${story.id}")
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
        storyRepository.save(engine.story)
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
        storyRepository.save(engine.story)
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
        storyRepository.save(engine.story)
    }

    @MessageMapping('/start')
    @RequestMapping(
            method = RequestMethod.POST,
            path = 'start',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    void start(@Payload @RequestBody StartRequest request) {
        Engine engine = requireEngine(request.storyId)
        engine.start()
    }

    private Engine requireEngine(storyId) throws ResponseStatusException {
        Engine engine = engineCache.get(UUID.fromString(storyId as String))
        if (!engine) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story ${storyId} not found")
        }
        engine
    }
}

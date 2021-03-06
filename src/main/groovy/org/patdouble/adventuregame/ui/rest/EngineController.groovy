package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.ErrorMessage
import org.patdouble.adventuregame.flow.GoalFulfilled
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.StoryEnded
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
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * REST controller for the lifecycle of an Engine.
 */
@RestController
@CompileDynamic
@RequestMapping('/api/engine')
@MessageMapping//('/engine')
@SuppressWarnings('Instanceof')
@Transactional
@Slf4j
class EngineController {
    @Autowired
    EngineCache engineCache
    @Autowired
    WorldRepository worldRepository
    @Autowired
    SimpMessageSendingOperations simpMessagingTemplate

    @SuppressWarnings('Unused')
    @MessageExceptionHandler
    ErrorMessage handleException(Exception e) {
        if (e instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) e
            return new ErrorMessage(httpCode: rse.status.value(), message: rse.reason ?: rse.status.reasonPhrase)
        }

        int code = HttpStatus.INTERNAL_SERVER_ERROR.value()
        if (e instanceof IllegalArgumentException) {
            code = HttpStatus.NOT_ACCEPTABLE.value()
        } else {
            log.error(e.message, e)
        }
        return new ErrorMessage(httpCode: code, message: e.message)
    }

    @SuppressWarnings('Unused')
    @ExceptionHandler
    @ResponseBody
    ErrorMessage handleExceptionHttp(Exception e, HttpServletResponse response) {
        ErrorMessage msg = handleException(e)
        response.sendError(msg.httpCode, msg.message)
        return msg
    }

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
            world = worldRepository.findByNameAndActive(request.worldName, true).first()
        }
        if (!world) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World ${request} not found")
        }
        Engine engine = engineCache.create(world)
        new CreateStoryResponse(storyUri: "/play/${engine.story.id}")
    }

    List<? extends StoryMessage> assembleState(Engine engine) {
        List<? extends StoryMessage> result = []
        result.addAll(engine.story.goals.findAll { it.fulfilled }.collect { new GoalFulfilled(it.goal) })
        if (engine.story.ended) {
            result << new StoryEnded()
        } else {
            result.addAll(engine.story.requests.collect { new RequestCreated(it) })
            result.addAll(engine.story.cast.collect { new PlayerChanged(it) })
            if (engine.story.chronos.current > 0) {
                result << new ChronosChanged(engine.story.chronos.current)
            }
        }
        result
    }

    @SubscribeMapping('/story.{storyId}')
    List<StoryMessage> subscribe(@DestinationVariable('storyId') @RequestParam('storyId') String storyId) {
        Engine engine = requireEngine(storyId)
        assembleState(engine)
    }

    @RequestMapping(
            method = RequestMethod.POST,
            path = 'state',
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<StoryMessage> state(@RequestParam('storyId') String storyId) {
        try {
            Engine engine = requireEngine(storyId)
            assembleState(engine)
        } catch (ResponseStatusException e) {
            if (e.status == HttpStatus.CONFLICT) {
                return [new StoryEnded()]
            }
            throw e
        }
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
        CompletableFuture<Boolean> future = engine.action(player, actionRequest.statement)
        if (actionRequest.waitForComplete) {
            try {
                boolean value = future.get(5, TimeUnit.SECONDS)
                if (!value) {
                    throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE)
                }
            } catch (TimeoutException e) {
                throw new ResponseStatusException(HttpStatus.PROCESSING)
            }
        }
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
        engine.addToCast(player).join()
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
        CompletableFuture<Void> future = engine.start(Motivator.AI)
        if (request.waitForComplete) {
            future.join()
        }
    }

    private Engine requireEngine(storyId) throws ResponseStatusException {
        Engine engine = engineCache.get(UUID.fromString(storyId as String))
        if (!engine) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story ${storyId} not found")
        }
        engine
    }
}

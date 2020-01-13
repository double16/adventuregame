package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.storage.jpa.StoryRepository
import org.patdouble.adventuregame.storage.jpa.WorldRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * REST controller for the lifecycle of an Engine.
 */
@RestController
@CompileDynamic
class EngineController {
    @Autowired
    WorldRepository worldRepository
    @Autowired
    StoryRepository storyRepository

    @RequestMapping(method = RequestMethod.POST, path = '/engine/create')
    String createStory(@RequestParam('world') String worldId) {
        World world
        List<World> worldsByName = worldRepository.findByName(worldId)
        if (worldsByName) {
            world = worldsByName.first()
        } else {
            world = worldRepository.findById(worldId as UUID).get()
        }
        if (!world) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        Story story = new Story(world)
        Engine engine = new Engine(story)
        engine.autoLifecycle = true
        engine.init()
        storyRepository.save(story)
        story.id as String
    }

    @RequestMapping(method = RequestMethod.POST, path = '/engine/resend')
    void resendRequests(@RequestParam('story') String storyId) {
        Engine engine = getEngine(storyId)
        engine.resendRequests()
    }

    @RequestMapping(method = RequestMethod.POST, path = '/engine/action')
    boolean action(
            @RequestParam('story') String storyId,
            @RequestParam('player') UUID playerId,
            @RequestParam('statement') String statement) {
        Engine engine = getEngine(storyId)
        Player player = engine.story.cast.find { it.id == playerId }
        if (!player) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        try {
            return engine.action(player, statement)
        } finally {
            storyRepository.save(engine.story)
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = '/engine/cast/add')
    void addToCast(@RequestParam('story') String storyId, Player player) {
        Engine engine = getEngine(storyId)
        engine.addToCast(player)
        storyRepository.save(engine.story)
    }

    @RequestMapping(method = RequestMethod.POST, path = '/engine/cast/ignore')
    void ignore(@RequestParam('story') String storyId, PlayerRequest request) {
        Engine engine = getEngine(storyId)
        engine.ignore(request)
        storyRepository.save(engine.story)
    }

    @RequestMapping(method = RequestMethod.POST, path = '/engine/close')
    void close(@RequestParam('story') String storyId) {
        Engine engine = getEngine(storyId)
        engine.close()
        storyRepository.save(engine.story)
    }

    private Engine getEngine(String storyId) {
        Optional<Story> story = storyRepository.findById(storyId as UUID)
        if (!story.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        if (story.get().ended) {
            throw new ResponseStatusException(HttpStatus.CONFLICT)
        }
        Engine engine = new Engine(story.get())
        engine.autoLifecycle = true
        return engine
    }
}

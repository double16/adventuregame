package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest

abstract class AbstractPlayerTest extends EngineTest {
    Player warrior
    Player thief

    abstract Motivator getDefaultMotivator()

    def setup() {
        engine.init()
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thiefRequest = story.requests.find { it.template.persona.name == 'thief' } as PlayerRequest
        warrior = warriorRequest.template.createPlayer(getDefaultMotivator())
        thief = thiefRequest.template.createPlayer(getDefaultMotivator())
        engine.addToCast(warrior)
        engine.addToCast(thief)
    }
}

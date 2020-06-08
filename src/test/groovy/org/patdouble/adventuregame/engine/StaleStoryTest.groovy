package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator

import static org.patdouble.adventuregame.SpecHelper.settle

class StaleStoryTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.AI
    }

    @Override
    void modify(World world) {
        world.goals.each {
            it.required = false
            it.theEnd = false
        }
        world.extras*.goals*.each { Goal g ->
            if (g.rules) {
                g.rules = [ 'player goes to room "nowhere"' ]
            }
        }
    }

    def setup() {
        engine.chronosLimit = 500
    }

    @Override
    boolean immobilizeAI() {
        false
    }

    def "stale story complets"() {
        when:
        engine.start().join()
        settle { engine.story.chronos.current }

        then:
        engine.story.ended
        engine.story.chronos.current == 41
    }
}

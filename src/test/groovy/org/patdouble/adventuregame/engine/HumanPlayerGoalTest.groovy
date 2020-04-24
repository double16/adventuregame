package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.StoryEnded
import org.patdouble.adventuregame.state.Motivator

import java.time.Duration

import static org.patdouble.adventuregame.SpecHelper.wait

class HumanPlayerGoalTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "fulfilled goal ends story"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'go north').join()
        engine.action(thief, 'wait').join()
        engine.action(warrior, 'go east').join()
        engine.action(thief, 'wait').join()
        engine.action(warrior, 'go east').join()

        then:
        warrior.room.modelId == 'trailer_4'
        and:
        engine.story.goals.find { it.goal.name == 'one' }.fulfilled
        engine.story.goals.find { it.goal.name == 'two' }.fulfilled
        !engine.story.goals.find { it.goal.name == 'three' }.fulfilled
        and:
        1 * storySubscriber.onNext(new StoryEnded())
    }
}

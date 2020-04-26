package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.GoalFulfilled
import org.patdouble.adventuregame.flow.StoryEnded
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.state.Motivator

class HumanPlayerGoalTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def "fulfilled goal ends story"() {
        given:
        engine.start().join()
        Goal goal1 = engine.story.goals.find { it.goal.name == 'one' }.goal
        Goal goal2 = engine.story.goals.find { it.goal.name == 'two' }.goal
        Goal goal3 = engine.story.goals.find { it.goal.name == 'three' }.goal

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
        1 * storySubscriber.onNext(new GoalFulfilled(goal: goal1))
        1 * storySubscriber.onNext(new GoalFulfilled(goal: goal2))
        0 * storySubscriber.onNext(new GoalFulfilled(goal: goal3))
        1 * storySubscriber.onNext(new StoryEnded())
    }
}

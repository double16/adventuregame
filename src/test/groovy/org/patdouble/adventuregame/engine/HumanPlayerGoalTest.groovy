package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.GoalFulfilled
import org.patdouble.adventuregame.flow.StoryEnded
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator

class HumanPlayerGoalTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    @Override
    void modify(World world) {
        super.modify(world)
        world.goals.find { it.name == 'two' }.theEnd = true
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

        then: 'player is in correct room'
        warrior.room.modelId == 'trailer_4'
        and: 'goals are marked fulfilled correctly'
        engine.story.goals.find { it.goal.name == 'one' }.fulfilled
        engine.story.goals.find { it.goal.name == 'two' }.fulfilled
        !engine.story.goals.find { it.goal.name == 'three' }.fulfilled
        and: 'goal events sent'
        1 * storySubscriber.onNext(new GoalFulfilled(goal: goal1))
        1 * storySubscriber.onNext(new GoalFulfilled(goal: goal2))
        0 * storySubscriber.onNext(new GoalFulfilled(goal: goal3))
        1 * storySubscriber.onNext(new StoryEnded())
        and: 'history recorded'
        engine.story.history.events.size() == 4
        !engine.story.history.events[0].players[0].player.is(engine.story.history.events[1].players[0].player)
        engine.story.history.events*.players*.action.flatten().size() == 40
    }
}

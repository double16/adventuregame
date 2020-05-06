package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import org.kie.api.definition.type.PropertyReactive
import org.patdouble.adventuregame.state.Story

/**
 * Holds the state of the story for the purpose of the rule engine.
 */
@PropertyReactive
@CompileStatic
class StoryState {
    private boolean ended

    /**
     * Initialize from story.
     */
    StoryState(Story story) {
        update(story)
    }

    boolean isEnded() {
        ended
    }

    /**
     * Update from story.
     */
    void update(Story story) {
        this.ended = story.ended
    }
}

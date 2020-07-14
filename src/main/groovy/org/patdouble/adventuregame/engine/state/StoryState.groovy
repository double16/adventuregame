package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.patdouble.adventuregame.state.Story

/**
 * Holds the state of the story for the purpose of the rule engine.
 */
@ToString(includePackage = false)
@CompileStatic
class StoryState {
    private boolean ended

    /**
     * Initialize from story.
     */
    StoryState(Story story) {
        update(story)
    }

    /**
     * Added to prevent both getRequired() and isRequired() from being generated which Drools
     * does not like.
     */
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

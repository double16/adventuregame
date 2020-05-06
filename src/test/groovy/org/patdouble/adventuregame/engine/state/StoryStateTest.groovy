package org.patdouble.adventuregame.engine.state

import org.patdouble.adventuregame.state.Story
import spock.lang.Specification

class StoryStateTest extends Specification {
    Story notEnded
    Story ended

    void setup() {
        notEnded = new Story()
        notEnded.ended = false
        ended = new Story()
        ended.ended = true
    }

    def "IsEnded"() {
        expect:
        !new StoryState(notEnded).isEnded()
        new StoryState(ended).isEnded()
    }

    def "Update"() {
        given:
        StoryState state = new StoryState(notEnded)

        when:
        state.update(notEnded)
        then:
        !state.isEnded()

        when:
        state.update(ended)
        then:
        state.isEnded()

        when:
        state.update(notEnded)
        then:
        !state.isEnded()
    }
}

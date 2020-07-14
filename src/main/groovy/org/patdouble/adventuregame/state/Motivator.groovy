package org.patdouble.adventuregame.state

import groovy.transform.CompileStatic

/**
 * What motivates a player? Human input or AI ?
 */
@CompileStatic
enum Motivator {
    HUMAN(false),
    AI(true),
    /** Temporary motivator to determine a hint for a human player. */
    AI_HINT(true)

    final boolean ai

    Motivator(boolean ai) {
        this.ai = ai
    }

    boolean isAi() {
        ai
    }
}

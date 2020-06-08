package org.patdouble.adventuregame.engine.state

import org.patdouble.adventuregame.state.Story

/**
 * Interface for providing a human readable string for an object.
 */
interface HasHumanString {
    CharSequence toHumanString(Story story)
}

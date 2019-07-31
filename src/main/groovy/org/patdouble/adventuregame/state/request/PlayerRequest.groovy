package org.patdouble.adventuregame.state.request

import groovy.transform.Canonical
import org.patdouble.adventuregame.model.PlayerTemplate

/**
 * Request for a player definition.
 */
@Canonical
class PlayerRequest implements Request {
    final PlayerTemplate template
    final boolean optional = false

    @Override
    String toString() {
        "${getClass().simpleName}(${template}${optional ? ' optional':''})"
    }
}

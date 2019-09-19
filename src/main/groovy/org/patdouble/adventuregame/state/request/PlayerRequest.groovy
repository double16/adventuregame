package org.patdouble.adventuregame.state.request

import groovy.transform.Canonical
import org.patdouble.adventuregame.model.PlayerTemplate

import javax.persistence.Entity
import javax.persistence.ManyToOne

/**
 * Request for a player definition.
 */
@Canonical
@Entity
class PlayerRequest extends Request {
    @ManyToOne
    final PlayerTemplate template
    final boolean optional = false

    @Override
    String toString() {
        "${getClass().simpleName}(${template}${optional ? ' optional' : ''})"
    }
}

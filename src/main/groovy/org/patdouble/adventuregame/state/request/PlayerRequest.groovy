package org.patdouble.adventuregame.state.request

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.PlayerTemplate

import javax.persistence.Entity
import javax.persistence.ManyToOne

/**
 * Request for a player definition.
 */
@Canonical
@Entity
@CompileDynamic
class PlayerRequest extends Request {
    @ManyToOne
    PlayerTemplate template
    boolean optional = false

    @Override
    String toString() {
        "${getClass().simpleName}(${template}${optional ? ' optional' : ''})"
    }
}

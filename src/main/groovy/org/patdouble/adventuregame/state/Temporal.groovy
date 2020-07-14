package org.patdouble.adventuregame.state

import groovy.transform.CompileStatic

/**
 * Marks state as existing in time. It's behavior will be subject to the Chronos.
 */
@CompileStatic
trait Temporal {
    long chronos
}

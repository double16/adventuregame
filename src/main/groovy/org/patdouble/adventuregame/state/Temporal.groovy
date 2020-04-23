package org.patdouble.adventuregame.state

import groovy.transform.CompileDynamic

/**
 * Marks state as existing in time. It's behavior will be subject to the Chronos.
 */
@CompileDynamic
trait Temporal {
    long chronos
}

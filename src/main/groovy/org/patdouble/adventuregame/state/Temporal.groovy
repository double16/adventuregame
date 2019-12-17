package org.patdouble.adventuregame.state

/**
 * Marks state as existing in time. It's behavior will be subject to the Chronos.
 */
trait Temporal {
    long chronos
}

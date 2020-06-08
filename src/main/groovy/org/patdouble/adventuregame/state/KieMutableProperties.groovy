package org.patdouble.adventuregame.state

/**
 * Provides a list of properties that may change during execution of the rule engine, thereby excluding those that
 * won't and improving performance.
 */
interface KieMutableProperties {
    /**
     * Return the list of properties that are subject to change. An empty list implies an immutable object.
     */
    String[] kieMutableProperties()
}

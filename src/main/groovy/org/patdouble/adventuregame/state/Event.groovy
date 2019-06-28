package org.patdouble.adventuregame.state

/**
 * An event in history. The event only records changes, not the entire current state.
 */
class Event {
    /** Tied to the Chronos value. */
    long when
    List<Player> cast = new ArrayList<>()
}

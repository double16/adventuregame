package org.patdouble.adventuregame.state

/**
 * An event in history. The event only records changes, not the entire current state.
 */
class Event {
    /** Tied to the {@link Chronos} value. */
    final long when
    final Collection<Player> cast

    Event(Collection<Player> players, Chronos chronos) {
        when = chronos.current
        cast = Collections.unmodifiableCollection(players*.clone())
    }
}

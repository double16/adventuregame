package org.patdouble.adventuregame.state

/**
 * The time keeper. The unit of time is not related to the clock but relative
 * to the human players actions. Each player must never be more than one unit
 * of time away from all the other players.
 */
class Chronos {
    long current = 0
}

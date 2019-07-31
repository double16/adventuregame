package org.patdouble.adventuregame.flow

import groovy.transform.Immutable

/**
 * Summary of a room at a point in time.
 */
@Immutable
class RoomSummary {
    long chronos
    String name
    String description
    String occupants
}

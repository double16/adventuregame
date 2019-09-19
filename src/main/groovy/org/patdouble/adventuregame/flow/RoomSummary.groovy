package org.patdouble.adventuregame.flow

import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable

import javax.persistence.Embeddable

/**
 * Summary of a room at a point in time.
 */
@Immutable
@Embeddable
@EqualsAndHashCode
class RoomSummary {
    long chronos
    String name
    String description
    String occupants
}

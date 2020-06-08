package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import org.kie.api.definition.type.ClassReactive

/**
 * Marks a room that a player has visited.
 */
@Immutable(includePackage = false)
@EqualsAndHashCode
@ClassReactive
@CompileStatic
class VisitedRoom {
    final UUID player
    final UUID room
    /** The last chronos visited. */
    final long when
    /** The number of visits. */
    final int count
}

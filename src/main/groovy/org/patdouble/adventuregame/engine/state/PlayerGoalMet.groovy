package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.kie.api.definition.type.ClassReactive

/**
 * Records a player-level goal as met. This is transient and intended to be produced by history.
 */
@Immutable(includePackage = false)
@ClassReactive
@CompileStatic
class PlayerGoalMet {
    final UUID player
    final UUID goal
    /** When the goal was met. */
    final long chronos
}

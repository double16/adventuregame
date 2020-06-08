package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.kie.api.definition.type.ClassReactive

/**
 * Records a single rule of a player-level goal as met. This is transient and intended to be produced by history, such
 * as the KnownRoom fact. (KnownRoom is subject to player memory limits and thus is more realistic.)
 */
@Immutable(includePackage = false)
@ClassReactive
@CompileStatic
class PlayerGoalRuleMet {
    final UUID player
    final UUID goal
    final String rule
    /** When the goal was met. */
    final long chronos
}

package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.kie.api.definition.type.ClassReactive

/**
 * A single goal for a player.
 */
@Immutable(includePackage = false)
@ClassReactive
@CompileStatic
class PlayerGoal {
    final UUID player
    final UUID goal
}

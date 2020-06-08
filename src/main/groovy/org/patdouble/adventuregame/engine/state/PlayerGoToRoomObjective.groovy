package org.patdouble.adventuregame.engine.state

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.kie.api.definition.type.ClassReactive

/**
 * A decomposition of a player goal to go to a room.
 */
@Canonical(includePackage = false, includeSuperProperties = true, callSuper = true, cache = true)
@ClassReactive
@CompileStatic
class PlayerGoToRoomObjective extends PlayerGoalRuleUnmet {
    final UUID room
}

package org.patdouble.adventuregame.state.request

import groovy.transform.Canonical
import groovy.transform.ToString
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.state.Player

/**
 * Requests the player to perform an action, i.e. a single 'move'.
 */
@Canonical(excludes = ['actions', 'directions'])
@ToString(includePackage = false, includeNames = true)
class ActionRequest implements Request {
    final Player player
    /** The chronos value that will be fulfilled by the action. */
    final long chronos
    final RoomSummary roomSummary
    /** The valid actions, may be a subset of the total. */
    List<String> actions = []
    /** The visible directions to neighbors. */
    List<String> directions = []
}

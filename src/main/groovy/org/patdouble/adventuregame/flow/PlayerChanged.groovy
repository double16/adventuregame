package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.patdouble.adventuregame.state.Player

/**
 * The state of the player has changed.
 */
@Canonical
@ToString(includePackage = false, includeNames = true)
@CompileStatic
class PlayerChanged extends StoryMessage {
    final Player player
    final long chronos
}

package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.ToString
import org.patdouble.adventuregame.state.Player

/**
 * Notification for a single player. Does not get included in the history.
 */
@Canonical
@ToString(includePackage = false, includeNames = true)
class PlayerNotification extends StoryMessage {
    final Player player
    final String subject
    final String text
}

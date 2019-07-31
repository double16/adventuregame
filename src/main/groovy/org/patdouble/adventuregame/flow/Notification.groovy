package org.patdouble.adventuregame.flow

import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Notification for all players. Does not get included in the history.
 */
@Immutable
@ToString(includePackage = false, includeNames = true)
class Notification extends StoryMessage {
    final String subject
    final String text
}

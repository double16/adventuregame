package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Notification for all players. Does not get included in the history.
 */
@Immutable
@ToString(includePackage = false, includeNames = true)
@CompileStatic
class Notification extends StoryMessage {
    String subject
    String text
}

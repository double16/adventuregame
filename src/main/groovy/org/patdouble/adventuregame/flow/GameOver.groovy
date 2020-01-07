package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * The story has ended, presumably because an end goal has been met.
 */
@Immutable
@ToString(includePackage = false)
@CompileStatic
class GameOver extends StoryMessage {
}

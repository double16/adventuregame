package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * The chronos value has been changed.
 */
@Immutable
@ToString(includePackage = false)
@CompileStatic
class ChronosChanged extends StoryMessage {
    /** The new current chronos value. */
    long current
}

package org.patdouble.adventuregame.flow

import groovy.transform.Immutable
import groovy.transform.ToString

@Immutable
@ToString(includePackage = false)
class ChronosChanged extends StoryMessage {
    /** The new current chronos value. */
    final long current
}

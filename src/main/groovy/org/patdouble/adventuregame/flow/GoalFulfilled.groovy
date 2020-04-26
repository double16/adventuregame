package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.patdouble.adventuregame.model.Goal

/**
 * Notification that a goal has been fulfilled.
 */
@Canonical
@ToString(includePackage = false, includeNames = true)
@CompileStatic
class GoalFulfilled extends StoryMessage {
    Goal goal
}

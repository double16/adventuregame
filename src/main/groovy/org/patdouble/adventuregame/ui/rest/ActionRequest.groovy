package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Request for
 * {@link org.patdouble.adventuregame.engine.Engine#action(org.patdouble.adventuregame.state.Player, java.lang.String)}
 */
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
@CompileStatic
class ActionRequest {
    String storyId
    String playerId
    String statement
    /** true if the call should not return until the action has finished. */
    Boolean waitForComplete
}

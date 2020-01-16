package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Request for {@link org.patdouble.adventuregame.engine.Engine#action(org.patdouble.adventuregame.state.Player, java.lang.String)}
 */
@Immutable
@CompileStatic
class ActionRequest {
    String storyId
    String playerId
    String statement
}

package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Request for {@link org.patdouble.adventuregame.engine.Engine#ignore(org.patdouble.adventuregame.state.request.PlayerRequest)}
 */
@Immutable
@CompileStatic
class IgnoreCastRequest {
    String storyId
    String playerTemplateId
}

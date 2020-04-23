package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Request for
 * {@link org.patdouble.adventuregame.engine.Engine#ignore(org.patdouble.adventuregame.state.request.PlayerRequest)}
 */
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
@CompileStatic
class IgnoreCastRequest {
    String storyId
    String playerTemplateId
}

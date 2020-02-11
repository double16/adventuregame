package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Request for {@link org.patdouble.adventuregame.engine.Engine#addToCast(org.patdouble.adventuregame.state.Player)}
 */
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
@CompileStatic
class AddToCastRequest {
    String storyId
    String playerTemplateId
    String motivator
    String fullName
    String nickName
}

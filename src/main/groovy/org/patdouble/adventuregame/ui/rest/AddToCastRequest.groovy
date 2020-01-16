package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Request for {@link org.patdouble.adventuregame.engine.Engine#addToCast(org.patdouble.adventuregame.state.Player)}
 */
@Immutable
@CompileStatic
class AddToCastRequest {
    String storyId
    String playerTemplateId
    String motivator
    String fullName
    String nickName
}

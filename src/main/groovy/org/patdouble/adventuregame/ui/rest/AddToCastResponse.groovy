package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Response for {@link org.patdouble.adventuregame.engine.Engine#addToCast(org.patdouble.adventuregame.state.Player)}
 */
@CompileStatic
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
class AddToCastResponse {
    String playerUri
}

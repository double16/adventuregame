package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Request for {@link EngineController#start(org.patdouble.adventuregame.ui.rest.StartRequest)}
 */
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
@CompileStatic
class StartRequest {
    String storyId
}

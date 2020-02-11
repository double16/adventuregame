package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Request for {@link EngineController#createStory(org.patdouble.adventuregame.ui.rest.CreateStoryRequest)}
 */
@CompileStatic
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
class CreateStoryResponse {
    String storyUri
}

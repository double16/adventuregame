package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Request for {@link EngineController#createStory(org.patdouble.adventuregame.ui.rest.CreateStoryRequest)}
 */
@Immutable
@ToString(cache = true, includeSuperProperties = true, includeNames = true, includePackage = false)
@CompileDynamic
class CreateStoryRequest {
    String worldId
    String worldName
}

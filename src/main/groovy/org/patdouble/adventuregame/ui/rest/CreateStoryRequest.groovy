package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.transform.Immutable

@Immutable
@CompileDynamic
class CreateStoryRequest {
    String worldId
    String worldName
}

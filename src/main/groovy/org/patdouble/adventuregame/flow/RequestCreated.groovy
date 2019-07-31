package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.ToString
import org.patdouble.adventuregame.state.request.Request

@Canonical
@ToString(includePackage = false)
class RequestCreated extends StoryMessage {
    final Request request
}

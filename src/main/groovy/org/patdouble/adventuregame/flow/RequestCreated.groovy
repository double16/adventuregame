package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.patdouble.adventuregame.state.request.Request

/**
 * Tells the UI that a request has been created. The receiver will need to decide if the request can be satisfied by
 * the user.
 */
@Canonical
@ToString(includePackage = false)
@CompileStatic
class RequestCreated extends StoryMessage {
    final Request request
}

package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.patdouble.adventuregame.state.request.Request

/**
 * Tells the UI that a request has been satisfied.
 */
@Canonical
@ToString(includePackage = false)
@CompileStatic
class RequestSatisfied extends StoryMessage {
    final Request request
}

package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * A generic error message modeled after HTTP error codes.
 */
@Immutable
@ToString(includePackage = false, includeNames = true)
@CompileStatic
class ErrorMessage extends StoryMessage {
    /** 404, 500, etc. */
    int httpCode
    String message
}

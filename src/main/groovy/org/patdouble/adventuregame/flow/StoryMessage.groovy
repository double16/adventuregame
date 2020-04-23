package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic

/**
 * Base for all messages published to the user.
 */
@CompileStatic
class StoryMessage {
    @SuppressWarnings('Unused')
    final String type = getClass().simpleName
    protected StoryMessage() { }
}

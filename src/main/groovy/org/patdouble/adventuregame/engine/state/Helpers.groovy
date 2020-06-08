package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.Engine

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Helper functions for rule matching.
 */
@CompileStatic
class Helpers {
    private static final Pattern RULE_ARGUMENT_PATTERN = Pattern.compile('["](.+?)["]')

    static String[] extractRoomModelId(CharSequence rule) {
        Matcher m = RULE_ARGUMENT_PATTERN.matcher(rule)
        m.findAll().collect { (it as String[])[1] } as String[]
    }

    static Closure<? extends CharSequence> stringify(Engine engine) {
        return { object ->
            if (object instanceof Collection) {
                return ((Collection) object).collect {
                    if (it instanceof HasHumanString) {
                        return it.toHumanString(engine.story)
                    }
                    it.toString()
                }.toString()
            } else if (object instanceof HasHumanString) {
                return ((HasHumanString) object).toHumanString(engine.story)
            }
            return object.toString()
        }
    }
}

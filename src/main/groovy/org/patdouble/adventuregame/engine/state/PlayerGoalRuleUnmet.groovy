package org.patdouble.adventuregame.engine.state

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Marks a fact as modelling an unmet player goal.
 */
@Canonical(includePackage = false, cache = true)
@CompileStatic
abstract class PlayerGoalRuleUnmet {
    boolean required
    UUID player

    /**
     * Added to prevent both getRequired() and isRequired() from being generated which Drools
     * does not like.
     */
    boolean isRequired() {
        required
    }
}

package org.patdouble.adventuregame.i18n

import groovy.transform.CompileDynamic
import groovy.transform.Immutable
import org.patdouble.adventuregame.model.Action

import javax.validation.constraints.NotNull

/**
 * Describes an action of a player.
 */
@Immutable
@CompileDynamic
class ActionStatement {
    @NotNull
    final String verb
    final String directObject
    final String indirectObject

    Action getVerbAsAction() {
        try {
            Action.valueOf(verb.toUpperCase())
        } catch (IllegalArgumentException e) {
            null
        }
    }

    @Override
    String toString() {
        if (indirectObject) {
            "${getClass().simpleName} '${verb} ${directObject} with ${indirectObject}'"
        } else if (directObject) {
            "${getClass().simpleName} '${verb} ${directObject}'"
        } else {
            "${getClass().simpleName} '${verb}'"
        }
    }
}

package org.patdouble.adventuregame.i18n

import groovy.transform.Immutable
import org.patdouble.adventuregame.model.Action

import javax.validation.constraints.NotNull

@Immutable
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

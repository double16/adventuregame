package org.patdouble.adventuregame.i18n

import groovy.transform.CompileDynamic
import groovy.transform.Immutable
import org.patdouble.adventuregame.model.Action

import javax.persistence.Embeddable
import javax.validation.constraints.NotNull

/**
 * Describes an action of a player.
 */
@Immutable
@Embeddable
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

    long getChronosCost() {
        long cost = 1
        Action a = getVerbAsAction()
        if (a) {
            cost = a.chronosCost
        }
        cost
    }

    String getText() {
        if (indirectObject) {
            "${verb} ${directObject} with ${indirectObject}"
        } else if (directObject) {
            "${verb} ${directObject}"
        } else {
            verb
        }
    }

    @Override
    String toString() {
        "${getClass().simpleName} '${text}'"
    }
}

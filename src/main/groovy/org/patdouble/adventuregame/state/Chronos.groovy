package org.patdouble.adventuregame.state

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

import javax.persistence.Column
import javax.persistence.Embeddable

import org.kie.api.definition.type.PropertyReactive

/**
 * The time keeper. The unit of time is not related to the clock but relative
 * to the human players actions. Each player must never be more than one unit
 * of time away from all the other players.
 */
@Embeddable
@EqualsAndHashCode
@PropertyReactive
@CompileStatic
class Chronos {
    @Column(name = 'chronos')
    long current = 0

    Chronos next() {
        current++
        this
    }

    @Override
    String toString() {
        current as String
    }
}

package org.patdouble.adventuregame.model

import spock.lang.Specification

class PersonaTest extends Specification {
    def "ToString"() {
        expect:
        PersonaMocks.THIEF.toString() == PersonaMocks.THIEF.name
        PersonaMocks.WARRIOR.toString() == PersonaMocks.WARRIOR.name
    }
}

package org.patdouble.adventuregame.state

import spock.lang.Specification

class ChronosTest extends Specification {
    def "Next"() {
        given:
        Chronos c = new Chronos()
        expect:
        c.current == 0
        c.next().current == 1
        c.next().current == 2
    }

    def "ToString"() {
        given:
        Chronos c = new Chronos()
        expect:
        c.toString() == '0'
        c.next().toString() == '1'
        c.next().toString() == '2'
    }
}

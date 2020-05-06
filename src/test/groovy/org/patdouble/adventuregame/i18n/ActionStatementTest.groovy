package org.patdouble.adventuregame.i18n

import org.patdouble.adventuregame.model.Action
import spock.lang.Specification

class ActionStatementTest extends Specification {
    def "GetVerbAsAction standard"() {
        expect:
        new ActionStatement(verb: 'go').getVerbAsAction() == Action.GO
    }

    def "GetVerbAsAction custom"() {
        expect:
        new ActionStatement(verb: 'custom').getVerbAsAction() == null
    }

    def "ToString"() {
        expect:
        new ActionStatement(verb: 'go').toString() == "ActionStatement 'go'"
        new ActionStatement(verb: 'go', directObject: 'north').toString() == "ActionStatement 'go north'"
        new ActionStatement(verb: 'attack', directObject: 'bear', indirectObject: 'knife').toString() == "ActionStatement 'attack bear with knife'"
    }

    def "GetText"() {
        expect:
        new ActionStatement(verb: 'go').text == "go"
        new ActionStatement(verb: 'go', directObject: 'north').text == "go north"
        new ActionStatement(verb: 'attack', directObject: 'bear', indirectObject: 'knife').text == "attack bear with knife"
    }

    def "GetChronosCost"() {
        expect:
        new ActionStatement(verb: 'go').chronosCost == 1
        new ActionStatement(verb: 'map').chronosCost == 0
        new ActionStatement(verb: 'custom').chronosCost == 1
    }
}

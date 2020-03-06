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
}

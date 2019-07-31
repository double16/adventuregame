package org.patdouble.adventuregame

import org.patdouble.adventuregame.model.Action
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ActionStatementParserTest extends Specification {
    ActionStatementParser parser

    def setup() {
        parser = new ActionStatementParser()
    }

    def "malformed"() {
        expect:
        parser.parse(null) == null
        parser.parse('') == null
    }

    def "verb only"() {
        expect:
        parser.parse(nls.toLowerCase()) == new ActionStatement(action, null, null)
        parser.parse(nls.toUpperCase()) == new ActionStatement(action, null, null)
        parser.parse(nls.capitalize()) == new ActionStatement(action, null, null)
        where:
        nls                | action
        Action.GO.name()   | 'go'
        Action.TAKE.name() | 'take'
    }

    def "verb synonym"() {
        expect:
        parser.parse(nls.toLowerCase()) == new ActionStatement(action, null, null)
        parser.parse(nls.toUpperCase()) == new ActionStatement(action, null, null)
        parser.parse(nls.capitalize()) == new ActionStatement(action, null, null)
        where:
        nls      | action
        'MOVE'   | 'go'
        'ATTACK' | 'fight'
    }

    def "verb direct object"() {
        expect:
        parser.parse('take pen') == new ActionStatement('take', 'pen', null)
        parser.parse('go n') == new ActionStatement('go', 'n', null)
        parser.parse('take   pen') == new ActionStatement('take', 'pen', null)
        parser.parse('take\tpen') == new ActionStatement('take', 'pen', null)
        parser.parse('take the pen') == new ActionStatement('take', 'pen', null)
    }

    def "verb direct and indirect objects"() {
        expect:
        parser.parse("fight slug with sprayer") == new ActionStatement('fight', 'slug', 'sprayer')
        parser.parse("fight the slug with sprayer") == new ActionStatement('fight', 'slug', 'sprayer')
        parser.parse("fight the slug with the sprayer") == new ActionStatement('fight', 'slug', 'sprayer')
        parser.parse("fight slug with the sprayer") == new ActionStatement('fight', 'slug', 'sprayer')
        parser.parse("fight slug using the sprayer") == new ActionStatement('fight', 'slug', 'sprayer')
        parser.parse("fight   the    slug    with    the    sprayer") == new ActionStatement('fight', 'slug', 'sprayer')
        parser.parse("fight slug using the large sprayer") == new ActionStatement('fight', 'slug', 'large sprayer')
        parser.parse('fight the slow slug using the large sprayer') == new ActionStatement('fight', 'slow slug', 'large sprayer')
    }

    def "verb direct object with article"() {
        expect:
        parser.parse('take pen') == new ActionStatement('take', 'pen', null)
        parser.parse('take   pen') == new ActionStatement('take', 'pen', null)
        parser.parse('take\tpen') == new ActionStatement('take', 'pen', null)
        parser.parse('take the pen') == new ActionStatement('take', 'pen', null)
        parser.parse('take the    pen') == new ActionStatement('take', 'pen', null)
        parser.parse('take a pen') == new ActionStatement('take', 'pen', null)
        parser.parse('pick up the pen') == new ActionStatement('take', 'pen', null)
        parser.parse('pick    up the    pen') == new ActionStatement('take', 'pen', null)
        parser.parse('pick up a pen') == new ActionStatement('take', 'pen', null)
    }
}

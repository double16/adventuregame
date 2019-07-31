package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.model.PersonaMocks
import spock.lang.Specification

class PlayerTest extends Specification {
    def "constructor"() {
        when:
        Player p = new Player(Motivator.AI, PersonaMocks.WARRIOR)
        then:
        p.persona == PersonaMocks.WARRIOR
        !p.persona.is(PersonaMocks.WARRIOR)
        p.motivator == Motivator.AI
        p.nickName == null
        p.fullName == null
        p.room == null
    }

    def "toString full"() {
        when:
        Player p = new Player(Motivator.AI, PersonaMocks.WARRIOR, 'shadowblade')
        then:
        p.toString() == 'shadowblade the warrior'
    }

    def "toString least"() {
        when:
        Player p = new Player(Motivator.AI, PersonaMocks.WARRIOR)
        then:
        p.toString() == 'warrior'
    }
}

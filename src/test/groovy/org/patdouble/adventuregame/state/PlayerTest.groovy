package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.model.PersonaMocks
import spock.lang.Specification

class PlayerTest extends Specification {
    Player p1a, p1b, p2a, p2b, p3

    def setup() {
        p1a = new Player(Motivator.AI, PersonaMocks.WARRIOR)
        p1b = new Player(Motivator.AI, PersonaMocks.WARRIOR)
        p2a = new Player(Motivator.AI, PersonaMocks.WARRIOR, 'shadowblade')
        p2b = new Player(Motivator.AI, PersonaMocks.WARRIOR, 'shadowblade')
        p3 = new Player(Motivator.AI, PersonaMocks.THIEF)
    }

    def "constructor"() {
        expect:
        p1a.persona == PersonaMocks.WARRIOR
        !p1a.persona.is(PersonaMocks.WARRIOR)
        p1a.motivator == Motivator.AI
        p1a.nickName == null
        p1a.fullName == null
        p1a.room == null
    }

    def "toStrings"() {
        expect:
        p1a.toString() == 'warrior'
        p2a.toString() == 'shadowblade the warrior'
    }

    def "equals"() {
        expect:
        p1a == p1a
        p1a == p1b
        p2a == p2a
        p2a == p2b
        p1a != p2a
        p1a != p2b
        p1b != p3
        p2b != p3
        and:
        p1a != ""
    }

    def "hashcode"() {
        expect:
        p1a.hashCode() == p1a.hashCode()
        p1a.hashCode() == p1b.hashCode()
        p2a.hashCode() == p2a.hashCode()
        p2a.hashCode() == p2b.hashCode()
    }

    def "computeSecureHash"() {
        expect:
        p1a.computeSecureHash() == p1a.computeSecureHash()
        p1a.computeSecureHash() == p1b.computeSecureHash()
        p1a.computeSecureHash() != p2a.computeSecureHash()
        p1a.computeSecureHash() != p3.computeSecureHash()
    }
}

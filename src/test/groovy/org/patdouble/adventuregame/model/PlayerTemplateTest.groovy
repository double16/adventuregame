package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import spock.lang.Specification

class PlayerTemplateTest extends Specification {
    PlayerTemplate template

    def setup() {
        template = new PlayerTemplate()
        template.persona = PersonaMocks.THIEF
        template.room = RoomMocks.ENTRANCE
    }

    def "CreatePlayer"() {
        when:
        Player p = template.createPlayer(Motivator.HUMAN)
        p.nickName = 'victor'
        p.fullName = 'victor the spider'
        then:
        p.motivator == Motivator.HUMAN
        p.persona == PersonaMocks.THIEF
        !p.persona.is(PersonaMocks.THIEF)
        p.nickName == 'victor'
        p.fullName == 'victor the spider'
        p.room == RoomMocks.ENTRANCE
    }

    def "ToString"() {
        expect:
        template.toString().contains('PlayerTemplate')
        template.toString().contains(' thief')
        template.toString().contains(' 1')
    }
}

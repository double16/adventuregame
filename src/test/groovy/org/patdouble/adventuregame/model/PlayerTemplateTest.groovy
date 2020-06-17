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
        template.goals.add(GoalMocks.PLAYER_ENTER_ROOM)
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
        p.template.goals.size() == 1
        p.template.goals[0].id == GoalMocks.PLAYER_ENTER_ROOM.id
    }

    def "ToString"() {
        expect:
        template.toString().contains('PlayerTemplate')
        template.toString().contains(' thief')
        template.toString().contains(' 1')
    }
    
    def "TestEquals"() {
        given:
        PlayerTemplate t1 = new PlayerTemplate()
        t1.persona = PersonaMocks.THIEF
        t1.room = RoomMocks.ENTRANCE
        t1.fullName = 'Victor the Spider'
        t1.nickName = 'Victor'
        and:
        PlayerTemplate t2 = new PlayerTemplate()
        t2.persona = PersonaMocks.WARRIOR
        t2.room = RoomMocks.ENTRANCE
        t2.fullName = 'Shadowblow the Hammer'
        t2.nickName = 'Shadowblow'

        expect:
        t1 == t1
        t1 != t2
    }

    def "TestHashCode"() {
        given:
        PlayerTemplate t1 = new PlayerTemplate()
        t1.persona = PersonaMocks.THIEF
        t1.room = RoomMocks.ENTRANCE
        t1.fullName = 'Victor the Spider'
        t1.nickName = 'Victor'

        expect:
        t1.hashCode() != template.hashCode()
        t1.hashCode() == t1.hashCode()
        template.hashCode() == template.hashCode()
    }
}

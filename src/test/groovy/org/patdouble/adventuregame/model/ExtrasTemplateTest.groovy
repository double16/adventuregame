package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import spock.lang.Specification

class ExtrasTemplateTest extends Specification {
    ExtrasTemplate template

    def setup() {
        template = new ExtrasTemplate()
        template.persona = PersonaMocks.THIEF
        template.quantity = 3
        template.room = RoomMocks.ENTRANCE
    }

    def "CreatePlayer"() {
        when:
        Player p = template.createPlayer()
        then:
        p.motivator == Motivator.AI
        p.persona == PersonaMocks.THIEF
        !p.persona.is(PersonaMocks.THIEF)
        p.nickName == null
        p.fullName == null
        p.room == RoomMocks.ENTRANCE
    }

    def "CreatePlayers"() {
        when:
        List<Player> players = template.createPlayers()
        then:
        players.size() == 3
        players*.motivator == [ Motivator.AI ]*3
        players*.persona == [ PersonaMocks.THIEF ]*3
        players*.nickName == [ null ]*3
        players*.fullName == [ null ]*3
        players*.room == [ RoomMocks.ENTRANCE ]*3
    }

    def "ToString"() {
        expect:
        template.toString().contains('ExtrasTemplate')
        template.toString().contains(' 3')
    }
}

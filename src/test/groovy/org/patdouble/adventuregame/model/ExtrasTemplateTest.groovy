package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import spock.lang.Specification

class ExtrasTemplateTest extends Specification {
    PlayerTemplate template

    def setup() {
        template = new PlayerTemplate()
        template.persona = PersonaMocks.THIEF
        template.quantity = 3..3
        template.room = RoomMocks.ENTRANCE
        template.goals.add(GoalMocks.PLAYER_ENTER_ROOM)
    }

    def "CreatePlayer"() {
        when:
        Player p = template.createPlayer(Motivator.AI)
        then:
        p.motivator == Motivator.AI
        p.persona == PersonaMocks.THIEF
        !p.persona.is(PersonaMocks.THIEF)
        p.nickName == null
        p.fullName == null
        p.room == RoomMocks.ENTRANCE
        p.template.goals.size() == 1
        p.template.goals[0].id == GoalMocks.PLAYER_ENTER_ROOM.id
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
        template.toString().contains('PlayerTemplate')
        template.toString().contains(' 3')
    }
}

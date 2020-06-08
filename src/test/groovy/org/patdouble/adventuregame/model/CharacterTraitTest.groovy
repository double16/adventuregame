package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import spock.lang.Specification

class CharacterTraitTest extends Specification {
    def "CreatePlayer"() {
        given:
        CharacterTrait ct = new PlayerTemplate()
        ct.persona = PersonaMocks.WARRIOR
        ct.nickName = 'S.B.'
        ct.fullName = 'shadowblade'
        ct.room = RoomMocks.ENTRANCE

        when:
        Player p = ct.createPlayer(Motivator.AI)

        then:
        p.persona == PersonaMocks.WARRIOR
        !p.persona.is(PersonaMocks.WARRIOR)
        p.nickName == 'S.B.'
        p.fullName == 'shadowblade'
        p.room == RoomMocks.ENTRANCE
    }
}

package org.patdouble.adventuregame.state.request

import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.RoomMocks
import spock.lang.Specification

class PlayerRequestTest extends Specification {
    PlayerTemplate template

    def setup() {
        template = new PlayerTemplate()
        template.persona = PersonaMocks.THIEF
        template.room = RoomMocks.ENTRANCE
    }

    def "ToString optional"() {
        given:
        PlayerRequest request = new PlayerRequest(template, true)
        expect:
        request.toString().contains('PlayerRequest')
        request.toString().contains('thief')
        request.toString().contains('optional')
    }

    def "ToString not optional"() {
        given:
        PlayerRequest request = new PlayerRequest(template, false)
        expect:
        request.toString().contains('PlayerRequest')
        request.toString().contains('thief')
        !request.toString().contains('optional')
    }
}

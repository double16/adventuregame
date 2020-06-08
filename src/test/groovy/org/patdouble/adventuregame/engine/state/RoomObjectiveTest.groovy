package org.patdouble.adventuregame.engine.state

import spock.lang.Specification

class RoomObjectiveTest extends Specification {
    UUID player1, player2
    UUID room1, room2

    def setup() {
        player1 = UUID.randomUUID()
        player2 = UUID.randomUUID()
        room1 = UUID.randomUUID()
        room2 = UUID.randomUUID()
    }

    def "equals method"() {
        expect:
        new RoomObjective(player1, room1, 1) == new RoomObjective(player1, room1, 1)
        new RoomObjective(player1, room1, 1) != new RoomObjective(player2, room1, 1)
        new RoomObjective(player1, room1, 1) != new RoomObjective(player1, room2, 1)
        new RoomObjective(player1, room1, 1) != new RoomObjective(player1, room1, 2)
    }

    def "hashCode method"() {
        expect:
        new RoomObjective(player1, room1, 1).hashCode() == new RoomObjective(player1, room1, 1).hashCode()
        new RoomObjective(player1, room1, 2).hashCode() != new RoomObjective(player1, room1, 1).hashCode()
    }

    def "sortable"() {
        expect:
        new RoomObjective(Collections.max([player1, player2]), room1, 2) < new RoomObjective(Collections.min([player1, player2]), room1, 3)
        new RoomObjective(player1, Collections.min([room1, room2]), 3) > new RoomObjective(player1, Collections.max([room1, room2]), 1)
        new RoomObjective(Collections.min([player1, player2]), room1, 1) < new RoomObjective(Collections.max([player1, player2]), room1, 1)
        new RoomObjective(player1, Collections.min([room1, room2]), 1) < new RoomObjective(player1, Collections.max([room1, room2]), 1)
    }
}

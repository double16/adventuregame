package org.patdouble.adventuregame.model

import spock.lang.Specification

class RoomTest extends Specification {
    public static final Room R1A = new Room(id: 'r1', name: 'one', description: 'room one')
    public static final Room R1B = new Room(id: 'r1', name: 'one', description: 'room one')
    public static final Room R2A = new Room(id: 'r2', name: 'two', description: 'room two')
    public static final Room R2Y = new Room(id: 'r2', name: 'two', description: 'room two B')
    public static final Room R2Z = new Room(id: 'r2', name: 'two B', description: 'room two B')

    def "HashCode"() {
        expect:
        R1A.hashCode() == R1B.hashCode()
        R1A.hashCode() != R2A.hashCode()
        R1A.hashCode() != R2Z.hashCode()
    }

    def "Equals"() {
        expect:
        R1A == R1B
        R1A != R2A
        R1A != R2Z
        R2A != R2Y
        R2A != R2Z
        and:
        !R1A.equals('r1')
    }

    def "Equals with rooms"() {
        given:
        Room r1 = new Room(id: 'r1')
        Room r2 = new Room(id: 'r1')
        Room r3 = new Room(id: 'r1')
        r1.addNeighbor('north', r2)
        r2.addNeighbor('south', r1)
        r1.addNeighbor('south', r3)
        r3.addNeighbor('north', r1)
        r2.addNeighbor('east', r3)
        r3.addNeighbor('west', r2)

        expect:
        r1 == r1
        r2 == r2
        r3 == r3
        and:
        r1 != r2
        r1 != r3
        r2 != r3
    }
}

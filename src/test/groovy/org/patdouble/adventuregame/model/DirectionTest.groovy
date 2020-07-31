package org.patdouble.adventuregame.model

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class DirectionTest extends Specification {

    void "opposite for #direction"() {
        expect:
        Direction.opposite(direction.name()).get() == opposite
        Direction.opposite(direction.name().toLowerCase()).get() == opposite

        where:
        direction           | opposite
        Direction.NORTH     | Direction.SOUTH
        Direction.NORTHEAST | Direction.SOUTHWEST
        Direction.NORTHWEST | Direction.SOUTHEAST
        Direction.SOUTH     | Direction.NORTH
        Direction.SOUTHEAST | Direction.NORTHWEST
        Direction.SOUTHWEST | Direction.NORTHEAST
        Direction.WEST      | Direction.EAST
        Direction.EAST      | Direction.WEST
        Direction.UP        | Direction.DOWN
        Direction.DOWN      | Direction.UP
    }

    void "opposite for invalid"() {
        expect:
        !Direction.opposite(null).isPresent()
        !Direction.opposite("").isPresent()
        !Direction.opposite("inside").isPresent()
    }
}

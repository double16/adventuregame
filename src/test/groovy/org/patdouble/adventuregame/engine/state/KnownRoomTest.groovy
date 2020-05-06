package org.patdouble.adventuregame.engine.state

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class KnownRoomTest extends Specification {

    def "MemoryLimitInChronos"() {
        expect:
        KnownRoom.memoryLimitInChronos(memoryLevel, visitCount) == result

        where:
        memoryLevel | visitCount | result
        0           | 1          | 0
        1           | 0          | 0
        0           | 0          | 0
        -1          | 0          | 0
        100         | -1         | 0
        500         | 1          | 83
        1000        | 100        | 1666
        250         | 1          | 20
    }
}

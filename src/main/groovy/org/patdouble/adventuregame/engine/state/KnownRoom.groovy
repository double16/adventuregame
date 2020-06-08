package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.Memoized
import org.kie.api.definition.type.ClassReactive

/**
 * Marks a room that of which a player is aware.
 */
@Immutable(includePackage = false)
@ClassReactive
@CompileStatic
class KnownRoom {
    @Memoized
    static int memoryLimitInChronos(int memoryLevel, int visitCount) {
        if (visitCount < 1) {
            return 0
        }
        if (memoryLevel < 1) {
            return 0
        }
        Math.floor((Math.pow(memoryLevel, 2) / 3000) * (2 * Math.log10((double) visitCount / 10) + 3))
    }

    final UUID player
    final UUID room
}

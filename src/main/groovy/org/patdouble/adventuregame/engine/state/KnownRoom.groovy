package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.Player

/**
 * Marks a room that a player has visited and remembers.
 */
@CompileStatic
class KnownRoom {
    static int memoryLimitInChronos(int memoryLevel, int visitCount) {
        if (visitCount < 1) {
            return 0
        }
        if (memoryLevel < 1) {
            return 0
        }
        Math.floor((Math.pow(memoryLevel, 2) / 3000) * (2 * Math.log10((double) visitCount / 10) + 3))
    }

    final Player player
    final Room room

    KnownRoom(Player player, Room room) {
        this.player = player
        this.room = room
    }
}

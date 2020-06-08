package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.kie.api.definition.type.ClassReactive
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.Story

import javax.validation.constraints.NotNull

/**
 * Describes the objective of a player reaching a room. There could be many of these in a route from the player's current
 * position to the destination.
 */
@TupleConstructor
@EqualsAndHashCode(cache = true)
@ToString(includePackage = false, includes = ['player', 'room', 'cost'])
@ClassReactive
@CompileStatic
class RoomObjective implements Comparable<RoomObjective>, HasHumanString {
    @NotNull
    final UUID player
    @NotNull
    final UUID room
    final int cost

    CharSequence toHumanString(Story story) {
        Player playerObj = story.cast.find { it.id == player }
        Room roomObj = story.world.rooms.find { it.id == room }
        "RoomObjective(${playerObj.title}, to ${roomObj.modelId}, cost ${cost})"
    }

    @Override
    int compareTo(RoomObjective o) {
        if (cost != o.cost) {
            return (cost < o.cost) ? -1 : 1
        }
        if (player != o.player) {
            return player <=> o.player
        }
        return room <=> o.room
    }
}

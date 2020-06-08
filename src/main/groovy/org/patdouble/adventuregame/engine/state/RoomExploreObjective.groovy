package org.patdouble.adventuregame.engine.state

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.kie.api.definition.type.ClassReactive
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.Story

/**
 * An objective for a player exploring for a room.
 */
@Immutable(includePackage = false)
@ClassReactive
@CompileStatic
class RoomExploreObjective implements HasHumanString {
    final UUID player
    /** The room from which we're exploring. */
    final UUID startRoom
    /** The direction FROM startRoom. */
    final String direction
    /** The room we will enter when in {@link #startRoom} and go {@link #direction}. */
    final UUID nextRoom
    /** The room we're trying to find. */
    final UUID targetRoom

    CharSequence toHumanString(Story story) {
        Player playerObj = story.cast.find { it.id == player }
        Room startRoomObj = story.world.rooms.find { it.id == startRoom }
        Room nextRoomObj = story.world.rooms.find { it.id == nextRoom }
        Room targetRoomObj = story.world.rooms.find { it.id == targetRoom }
        "RoomExploreObjective(${playerObj.title}, from ${startRoomObj.modelId} to ${nextRoomObj.modelId} via ${direction} to find ${targetRoomObj.modelId})"
    }
}

package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.i18n.Bundles
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.request.Request

/**
 * The current state of a story. The world, characters, positions, history, ...
 *
 * This is the top-level state object. It is initialized and maintained by an {@link org.patdouble.adventuregame.engine.Engine}.
 */
class Story {
    /** The world from which this stage was set. */
    final World world
    Collection<Player> cast = []
    Chronos chronos
    History history
    Collection<Request> requests = []

    Story(World world) {
        this.world = world
    }

    Collection<Player> getPlayers(Room room = null) {
        cast.findAll { it.nickName && (!room || it.room == room ) }
    }

    Map<Persona, List<Player>> getExtras(Room room = null) {
        cast.findAll { !it.nickName && (!room || it.room == room ) }.groupBy { it.persona }
    }

    RoomSummary roomSummary(Room room, Player player, Bundles bundles) {
        Collection<Player> players = getPlayers(room) - [player]
        Map<Persona, List<Player>> extras = getExtras(room)
        String occupants = null
        if (!players.empty || !extras.isEmpty()) {
            occupants = bundles.roomsummaryTextTemplate.make([players: players, extras: extras]).toString()
        }
        StringBuilder description = new StringBuilder(room.description)
        List<String> directions = room.neighbors.keySet().sort()
        if (directions) {
            if (!description.endsWithAny('.')) {
                description.append('. ')
            }
            else if (!description.endsWithAny(' ')) {
                description.append(' ')
            }
            description.append(bundles.roomsummaryDirectionsTemplate.make([directions: directions]).toString()).append('.')
        }
        new RoomSummary(chronos.current, room.name, description as String, occupants)
    }
}

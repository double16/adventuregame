package org.patdouble.adventuregame.state

import groovy.transform.EqualsAndHashCode
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.i18n.Bundles
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.request.Request

import javax.persistence.CascadeType
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne

/**
 * The current state of a story. The world, characters, positions, history, ...
 *
 * This is the top-level state object. It is initialized and maintained by an {@link org.patdouble.adventuregame.engine.Engine}.
 */
@Entity
@EqualsAndHashCode(excludes = ['id'])
class Story {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    /** The world from which this stage was set. */
    @ManyToOne
    World world
    @OneToMany(cascade = CascadeType.ALL)
    Collection<Player> cast = []
    // FIXME: unit tests
    @OneToMany(cascade = CascadeType.ALL)
    Collection<GoalStatus> goals = []
    @Embedded
    Chronos chronos
    @OneToOne(cascade = CascadeType.ALL)
    History history
    @OneToMany(cascade = CascadeType.ALL)
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

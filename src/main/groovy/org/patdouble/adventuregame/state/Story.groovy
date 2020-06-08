package org.patdouble.adventuregame.state

import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.i18n.Bundles
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.request.Request
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import java.time.LocalDateTime

/**
 * The current state of a story. The world, characters, positions, history, ...
 *
 * This is the top-level state object. It is initialized and maintained by an
 * {@link org.patdouble.adventuregame.engine.Engine}.
 */
@Entity
@EqualsAndHashCode(excludes = [Constants.COL_ID, 'created', 'modified'])
@ToString(includePackage = false, includes = ['id', 'world', 'created', 'ended'])
@CompileDynamic
class Story implements KieMutableProperties {
    private static final String[] KIE_MUTABLE_PROPS = [
            'history',
            // GoalStatus is managed separately
            // 'goals',
            'chronos',
            'ended',
            'requests',
    ]

    public static final String FULL_STOP = '.'
    public static final String SPACE = ' '

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    /** The world from which this stage was set. */
    @ManyToOne(cascade = CascadeType.MERGE)
    World world
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    List<Player> cast = []
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    List<GoalStatus> goals = []
    @Embedded
    Chronos chronos
    /** True if the story has ended. */
    boolean ended
    @OneToOne(cascade = CascadeType.ALL)
    History history
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    List<Request> requests = []
    @CreationTimestamp
    @SuppressWarnings('Unused')
    LocalDateTime created
    @UpdateTimestamp
    @SuppressWarnings('Unused')
    LocalDateTime modified

    Story() { }

    Story(World world) {
        this.world = world
    }

    @Override
    String[] kieMutableProperties() {
        KIE_MUTABLE_PROPS
    }

    Collection<Player> getPlayers(Room room = null) {
        cast.findAll { it.nickName && (!room || it.room == room ) }
    }

    Map<String, List<Player>> getExtras(Room room = null) {
        cast.findAll { !it.nickName && (!room || it.room == room ) }.groupBy { it.persona.name }
    }

    RoomSummary roomSummary(Room room, Player player, Bundles bundles) {
        Collection<Player> players = getPlayers(room) - [player]
        Map<String, List<Player>> extras = getExtras(room)
        String occupants = null
        // DO NOT use property version, it doesn't work for Map.isEmpty()
        if (!players.isEmpty() || !extras.isEmpty()) {
            occupants = bundles.roomsummaryTextTemplate.make([players: players, extras: extras]).toString()
        }
        StringBuilder description = new StringBuilder(room.description)
        List<String> directions = room.neighbors.keySet().sort()
        if (directions) {
            if (!description.endsWithAny(FULL_STOP)) {
                description.append('. ')
            }
            else if (!description.endsWithAny(SPACE)) {
                description.append(SPACE)
            }
            description
                    .append(bundles.roomsummaryDirectionsTemplate.make([directions: directions]).toString())
                    .append(FULL_STOP)
        }
        new RoomSummary(chronos.current, room.name, description as String, occupants)
    }

    /**
     * Fully initialize the entity.
     * @param story attached entity to initialize
     * @return the story instance
     */
    Story initialize() {
        Hibernate.initialize(world)
        world.initialize()
        Hibernate.initialize(cast)
        cast*.initialize()
        Hibernate.initialize(goals)
        Hibernate.initialize(requests)
        requests*.initialize()
        if (history) {
            Hibernate.initialize(history)
            history.initialize()
        }
        this
    }
}

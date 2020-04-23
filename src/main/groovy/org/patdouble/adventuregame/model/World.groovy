package org.patdouble.adventuregame.model

import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.hibernate.Hibernate
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.OneToMany

/**
 * Models the world for the starting point of one or more stories.
 *
 * This is the top level model object.
 */
@Entity
@EqualsAndHashCode(excludes = [Constants.COL_ID])
@CompileDynamic
class World {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    String name
    String author
    @Lob
    String description

    @OneToMany(cascade = CascadeType.ALL)
    List<Persona> personas = []

    /**
     * List of available players.
     */
    @OneToMany(cascade = CascadeType.ALL)
    List<PlayerTemplate> players = []

    /**
     * List of extras, which are like players but are only controlled by AI.
     */
    @OneToMany(cascade = CascadeType.ALL)
    List<ExtrasTemplate> extras = []

    @OneToMany(cascade = CascadeType.ALL)
    List<Room> rooms = []

    @OneToMany(cascade = CascadeType.ALL)
    List<Goal> goals = []

    Optional<Room> findRoomById(String id) {
        getRooms().stream().filter { it.modelId == id }.findFirst()
    }

    World initialize() {
        Hibernate.initialize(personas)
        Hibernate.initialize(players)
        players*.initialize()
        Hibernate.initialize(extras)
        extras*.initialize()
        Hibernate.initialize(rooms)
        rooms*.initialize()
        Hibernate.initialize(goals)
        this
    }
}

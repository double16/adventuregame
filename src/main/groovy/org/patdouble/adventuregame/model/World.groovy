package org.patdouble.adventuregame.model

import groovy.transform.EqualsAndHashCode

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

/**
 * Models the world for the starting point of one or more stories.
 *
 * This is the top level model object.
 */
@Entity
@EqualsAndHashCode(excludes = ['id'])
class World {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    String name
    String author
    String description

    @OneToMany(cascade = CascadeType.ALL)
    final List<Persona> personas = []

    /**
     * List of available players.
     */
    @OneToMany(cascade = CascadeType.ALL)
    final List<PlayerTemplate> players = []

    /**
     * List of extras, which are like players but are only controlled by AI.
     */
    @OneToMany(cascade = CascadeType.ALL)
    final List<ExtrasTemplate> extras = []

    @OneToMany(cascade = CascadeType.ALL)
    final List<Room> rooms = []

    // FIXME: Parse, unit tests
    @OneToMany(cascade = CascadeType.ALL)
    final List<Goal> goals = []

//    final List<Challenge> challenges = []

    Optional<Room> findRoomById(String id) {
        getRooms().stream().filter { it.id == id }.findFirst()
    }
}

package org.patdouble.adventuregame.model

import groovy.transform.CompileDynamic
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
@CompileDynamic
class World {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    String name
    String author
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
        getRooms().stream().filter { it.id == id }.findFirst()
    }
}

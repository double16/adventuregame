package org.patdouble.adventuregame.state

import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.patdouble.adventuregame.model.World

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * Records the history of the story. Used for manuscript generation.
 */
@Entity
@EqualsAndHashCode(excludes = ['id'])
@CompileDynamic
class History {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    @ManyToOne
    World world
    @OneToMany(cascade = CascadeType.ALL)
    List<Event> events = []

    History() { }

    History(World world) {
        this.world = world
    }

    void addEvent(Event e) {
        events << e
    }
}

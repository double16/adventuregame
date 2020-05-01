package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.storage.jpa.Constants

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
@EqualsAndHashCode(excludes = [Constants.COL_ID, Constants.COL_DBID])
@CompileDynamic
class History {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @ManyToOne
    World world
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    List<Event> events = []

    History() { }

    History(World world) {
        this.world = world
    }

    void addEvent(Event e) {
        events << e
    }
}

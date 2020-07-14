package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.hibernate.Hibernate
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
@EqualsAndHashCode(excludes = [Constants.COL_DBID])
@ToString
@CompileStatic
class History {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId

    @ManyToOne
    World world
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = 'history')
    List<Event> events = []

    History() { }

    History(World world) {
        this.world = world
    }

    void addEvent(Event e) {
        e.history = this
        events << e
    }

    History initialize() {
        Hibernate.initialize(events)
        events*.initialize()
        this
    }
}

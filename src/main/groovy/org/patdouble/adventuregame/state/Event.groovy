package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.hibernate.Hibernate
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * An event in history. The event only records changes, not the entire state.
 */
@Entity
@EqualsAndHashCode(excludes = [Constants.COL_DBID, 'history'])
@CompileDynamic
class Event {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @ManyToOne
    @JsonIgnore
    History history

    /** Tied to the {@link Chronos} value. */
    long when

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    List<PlayerEvent> players = []

    Event() { }

    Event(Chronos chronos) {
        when = chronos.current
    }

    Event initialize() {
        Hibernate.initialize(players)
        players*.initialize()
        this
    }
}

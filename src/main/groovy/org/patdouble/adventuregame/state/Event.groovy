package org.patdouble.adventuregame.state

import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

/**
 * An event in history. The event only records changes, not the entire current state.
 */
@Entity
@EqualsAndHashCode(excludes = ['id'])
@CompileDynamic
class Event {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    /** Tied to the {@link Chronos} value. */
    long when
    @OneToMany(cascade = CascadeType.ALL)
    Collection<Player> cast

    Event() { }

    Event(Collection<Player> players, Chronos chronos) {
        when = chronos.current
        cast = Collections.unmodifiableCollection(players*.clone())
    }
}

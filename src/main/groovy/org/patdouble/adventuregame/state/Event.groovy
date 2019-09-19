package org.patdouble.adventuregame.state

import groovy.transform.EqualsAndHashCode

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * An event in history. The event only records changes, not the entire current state.
 */
@Entity
@EqualsAndHashCode(excludes = ['id'])
class Event {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    /** Tied to the {@link Chronos} value. */
    final long when
    @OneToMany(cascade = CascadeType.ALL)
    final Collection<Player> cast

    Event(Collection<Player> players, Chronos chronos) {
        when = chronos.current
        cast = Collections.unmodifiableCollection(players*.clone())
    }
}

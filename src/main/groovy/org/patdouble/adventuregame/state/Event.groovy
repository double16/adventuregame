package org.patdouble.adventuregame.state

import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany

/**
 * An event in history. The event only records changes, not the entire state.
 */
@Entity
@EqualsAndHashCode(excludes = [Constants.COL_ID])
@CompileDynamic
class Event {
    @Id
    UUID id = UUID.randomUUID()

    /** Tied to the {@link Chronos} value. */
    long when
    @OneToMany(cascade = CascadeType.ALL)
    List<Player> cast = []

    Event() { }

    Event(Collection<Player> players, Chronos chronos) {
        when = chronos.current
        cast = Collections.unmodifiableCollection(players*.clone())
    }
}

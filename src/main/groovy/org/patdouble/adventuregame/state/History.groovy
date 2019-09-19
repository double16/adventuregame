package org.patdouble.adventuregame.state

import groovy.transform.EqualsAndHashCode
import org.patdouble.adventuregame.model.World

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
@EqualsAndHashCode(excludes = ['id'])
class History {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    @ManyToOne
    World world
    @OneToMany(cascade = CascadeType.ALL)
    List<Event> events = []

    History(World world) {
        this.world = world
    }

    void addEvent(Event e) {
        events << e
    }
}

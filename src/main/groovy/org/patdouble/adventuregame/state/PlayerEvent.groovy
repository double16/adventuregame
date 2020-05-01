package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.hibernate.Hibernate
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

/**
 * Record the state of a player and the action taken during a chronos.
 */
@Canonical(excludes = [Constants.COL_DBID, 'event'], includePackage = false)
@Entity
@CompileDynamic
class PlayerEvent {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @ManyToOne
    @JsonIgnore
    Event event
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    Player player
    @Embedded
    ActionStatement action

    PlayerEvent initialize() {
        Hibernate.initialize(player)
        this
    }
}

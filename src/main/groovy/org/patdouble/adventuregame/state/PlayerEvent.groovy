package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sun.istack.NotNull
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.hibernate.Hibernate
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.model.CanSecureHash
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import java.security.MessageDigest

/**
 * Record the state of a player and the action taken during a chronos.
 */
@Canonical(excludes = [Constants.COL_DBID, 'event'], includePackage = false)
@Entity
@TupleConstructor
@CompileStatic
class PlayerEvent implements KieMutableProperties, CanSecureHash {
    private static final String[] KIE_MUTABLE_PROPS = []

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @ManyToOne
    @JsonIgnore
    @NotNull
    Event event
    @NotNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    Player player
    @Embedded
    ActionStatement action

    @Override
    String[] kieMutableProperties() {
        KIE_MUTABLE_PROPS
    }

    PlayerEvent initialize() {
        Hibernate.initialize(player)
        this
    }

    @Override
    void update(MessageDigest md) {
        player.update(md)
        action?.update(md)
    }
}

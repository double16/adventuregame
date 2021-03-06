package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle
import groovy.transform.CompileDynamic
import org.hibernate.Hibernate
import org.patdouble.adventuregame.model.CanSecureHash
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.validation.constraints.NotNull
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Models the player's attributes and current state.
 */
@AutoClone(excludes = [Constants.COL_DBID], style = AutoCloneStyle.COPY_CONSTRUCTOR)
@Entity
@CompileDynamic
class Player implements Temporal, KieMutableProperties, CanSecureHash {
    private static final String[] KIE_MUTABLE_PROPS = Persona.KIE_MUTABLE_PROPS + ['chronos', 'room'] as String[]

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @ManyToOne
    @NotNull
    PlayerTemplate template

    Motivator motivator
    @Delegate(excludes = [ 'clone', 'update', 'computeSecureHash', Constants.COL_DBID, Constants.COL_ID ])
    @OneToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    Persona persona = new Persona()
    /** Unique number per Persona when multiple players use the same Persona. */
    int siblingNumber = 1
    String nickName
    String fullName
    /** The location of the player. */
    @ManyToOne
    Room room

    Player() { }

    Player(PlayerTemplate template, Motivator motivator, Persona persona, String nickName = null) {
        this.template = template
        this.motivator = motivator
        this.nickName = nickName
        this.persona = persona.clone()
        Objects.requireNonNull(this.persona.name)
    }

    String[] kieMutableProperties() {
        KIE_MUTABLE_PROPS
    }

    @JsonIgnore
    String getTitle() {
        if (nickName) {
            "${nickName} the ${persona.name.toLowerCase()}"
        } else if (siblingNumber > 1) {
            "${persona.name.toLowerCase()} ${siblingNumber}"
        } else {
            persona.name.toLowerCase()
        }
    }

    @Override
    String toString() {
        getTitle()
    }

    @Override
    @SuppressWarnings('Instanceof')
    boolean equals(Object obj) {
        if (!(obj instanceof Player)) {
            return false
        }
        Player other = obj as Player
        return persona.name == other.persona.name &&
                siblingNumber == other.siblingNumber &&
                nickName == other.nickName &&
                motivator == other.motivator
    }

    @Override
    int hashCode() {
        Objects.hash(persona.name, siblingNumber, nickName, motivator)
    }

    Player initialize() {
        Hibernate.initialize(template)
        Hibernate.initialize(persona)
        Hibernate.initialize(room)
        this
    }

    Player cloneKeepId() {
        Player p = clone()
        p.id = this.id
        p
    }

    @Override
    void update(MessageDigest md) {
        persona.update(md)
        md.update(motivator.name().bytes)
        if (nickName) {
            md.update(nickName.bytes)
        }
        if (fullName) {
            md.update(fullName.bytes)
        }
        if (room) {
            md.update(room.modelId.bytes)
        }

        ByteBuffer intb = ByteBuffer.allocate(4)
        md.update(intb.rewind().putInt(siblingNumber).array())
    }
}

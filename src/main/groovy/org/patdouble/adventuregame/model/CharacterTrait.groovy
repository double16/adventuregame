package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.hibernate.Hibernate
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderColumn
import javax.validation.constraints.NotNull
import java.security.MessageDigest

/**
 * Common traits for characters.
 */
@CompileDynamic
@EqualsAndHashCode
trait CharacterTrait implements CanSecureHash {
    @Delegate(excludes = [ 'clone', 'update', 'computeSecureHash', Constants.COL_DBID, Constants.COL_ID ])
    @ManyToOne
    @NotNull
    @JsonIgnore
    Persona persona = new Persona()
    String nickName
    String fullName
    @ManyToOne
    @NotNull
    Room room
    /** List of rooms that are always known to the player, i.e. long term memory. */
    @ManyToMany
    List<Room> knownRooms = []
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name="INDEX")
    List<Goal> goals = []

    Player createPlayer(@NotNull Motivator motivator) {
        Player p = new Player(motivator, persona, nickName)
        p.fullName = fullName
        p.room = room
        p.knownRooms.addAll(knownRooms)
        p.goals.addAll(goals)
        p
    }

    CharacterTrait initialize() {
        Hibernate.initialize(persona)
        Hibernate.initialize(room)
        Hibernate.initialize(knownRooms)
        Hibernate.initialize(goals)
        this
    }

    void update(MessageDigest md) {
        persona.update(md)
        md.update((nickName ?: "").bytes)
        md.update((fullName ?: "").bytes)
        md.update((room?.modelId ?: "").bytes)
        knownRooms.each { md.update(it.modelId.bytes) }
        goals*.update(md)
    }
}

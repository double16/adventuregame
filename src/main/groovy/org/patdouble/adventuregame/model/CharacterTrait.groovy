package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import org.hibernate.Hibernate
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

/**
 * Common traits for characters.
 */
@CompileDynamic
@EqualsAndHashCode
trait CharacterTrait {
    @Delegate(excludes = [ 'clone', Constants.COL_DBID, Constants.COL_ID ])
    @ManyToOne
    @NotNull
    @JsonIgnore
    Persona persona = new Persona()
    String nickName
    String fullName
    @ManyToOne
    @NotNull
    Room room

    Player createPlayer(@NotNull Motivator motivator) {
        Player p = new Player(motivator, persona, nickName)
        p.fullName = fullName
        p.room = room
        p
    }

    CharacterTrait initialize() {
        Hibernate.initialize(persona)
        Hibernate.initialize(room)
        this
    }
}

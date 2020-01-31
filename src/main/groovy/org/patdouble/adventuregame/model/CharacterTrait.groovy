package org.patdouble.adventuregame.model

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player

import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

/**
 * Common traits for characters.
 */
@CompileDynamic
trait CharacterTrait {
    @Delegate(excludes = [ 'clone' ])
    @ManyToOne
    @NotNull
    Persona persona
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
}

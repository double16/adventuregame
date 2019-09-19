package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player

import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

trait CharacterTrait {
    @Delegate
    @ManyToOne
    @NotNull
    Persona persona
    String nickName
    String fullName
    @ManyToOne
    @NotNull
    Room room

    Player createPlayer(@NotNull Motivator motivator) {
        Player p = new Player(motivator, persona.clone(), nickName)
        p.fullName = fullName
        p.room = room
        p
    }
}

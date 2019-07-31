package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player

trait CharacterTrait {
    @Delegate
    Persona persona
    String nickName
    String fullName
    Room room

    Player createPlayer(Motivator motivator) {
        Player p = new Player(motivator, persona.clone(), nickName)
        p.fullName = fullName
        p.room = room
        p
    }
}

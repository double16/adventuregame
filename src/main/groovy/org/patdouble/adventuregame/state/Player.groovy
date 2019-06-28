package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.Room

/**
 * Models the player's attributes and current state.
 */
class Player {
    Motivator motivator
    @Delegate
    Persona persona
    String nickName
    String fullName
    /** The location of the player. */
    Room room

    Player(Motivator motivator, Persona persona, String nickName) {
        this.motivator = motivator
        this.nickName = nickName
        this.persona = persona.clone()
    }

    String toString() {
        "$nickName the ${persona.getName().toLowerCase()}"
    }

    String getStatus() {
        "${toString()} is at ${persona.getHealth()}% health and has \$${persona.getWealth()}"
    }
}

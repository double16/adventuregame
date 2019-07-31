package org.patdouble.adventuregame.state

import groovy.transform.AutoClone
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.Room

/**
 * Models the player's attributes and current state.
 */
@AutoClone
class Player {
    Motivator motivator
    @Delegate(excludes = [ 'clone' ])
    Persona persona
    String nickName
    String fullName
    /** The location of the player. */
    Room room

    Player(Motivator motivator, Persona persona, String nickName = null) {
        this.motivator = motivator
        this.nickName = nickName
        this.persona = persona.clone()
    }

    String getTitle() {
        if (nickName) {
            "${nickName} the ${persona.getName().toLowerCase()}"
        } else {
            persona.getName().toLowerCase()
        }
    }

    @Override
    String toString() {
        getTitle()
    }

    String getStatus() {
        "${toString()} is at ${persona.getHealth()}% health and has \$${persona.getWealth()}"
    }

    @Override
    boolean equals(Object obj) {
        if (!obj instanceof Player) {
            return false
        }
        Player other = obj as Player
        return persona.name == other.persona.name &&
                nickName == other.nickName &&
                motivator == other.motivator
    }
}

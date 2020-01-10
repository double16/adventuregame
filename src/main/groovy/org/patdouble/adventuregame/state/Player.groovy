package org.patdouble.adventuregame.state

import groovy.transform.AutoClone
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.Room

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Models the player's attributes and current state.
 */
@AutoClone(excludes = ['id'])
@Entity
@CompileDynamic
class Player implements Temporal {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    Motivator motivator
    @Delegate(excludes = [ 'clone' ])
    @ManyToOne
    Persona persona
    String nickName
    String fullName
    /** The location of the player. */
    @ManyToOne
    Room room

    Player() { }

    Player(Motivator motivator, Persona persona, String nickName = null) {
        this.motivator = motivator
        this.nickName = nickName
        this.persona = persona.clone()
    }

    String getTitle() {
        if (nickName) {
            "${nickName} the ${persona.name.toLowerCase()}"
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
                nickName == other.nickName &&
                motivator == other.motivator
    }

    @Override
    int hashCode() {
        Objects.hash(persona.name, nickName, motivator)
    }
}

package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

/**
 * Template for extras in the story. Extras are always controlled by AI. An extra may be an important role, such as a
 * villain to defeat.
 */
@Canonical(excludes = [Constants.COL_ID, Constants.COL_DBID])
@Entity
@CompileDynamic
class ExtrasTemplate implements CharacterTrait {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    /** The number of extras. */
    int quantity = 1

    Player createPlayer() {
        createPlayer(Motivator.AI)
    }

    List<Player> createPlayers() {
        List<Player> players = []
        int c = quantity
        while (c-- > 0) {
            players << createPlayer()
        }
        players
    }

    @Override
    @JsonIgnore
    Persona getPersona() {
        CharacterTrait.super.getPersona()
    }

    @Override
    String toString() {
        "ExtrasTemplate: ${persona.toString()}, nick ${nickName}, full ${fullName}, room ${room.modelId}, qty ${quantity}"
    }
}

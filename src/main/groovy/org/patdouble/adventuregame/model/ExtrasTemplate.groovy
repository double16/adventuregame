package org.patdouble.adventuregame.model

import groovy.transform.Canonical
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player

/**
 * Template for extras in the story. Extras are always controlled by AI. An extra may be an important role, such as a
 * villain to defeat.
 */
@Canonical
class ExtrasTemplate implements CharacterTrait {
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
    String toString() {
        "ExtrasTemplate: ${persona.toString()}, nick ${nickName}, full ${fullName}, room ${room.id}, qty ${quantity}"
    }
}

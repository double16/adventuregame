package org.patdouble.adventuregame.model

import groovy.transform.Canonical
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player

import javax.validation.constraints.NotNull

/**
 * Template for players in the story. Players can either be human or AI.
 */
@Canonical
class PlayerTemplate implements CharacterTrait {
    /** The allowed quantity of this type of player. */
    Range<Integer> quantity = 1..1

    Player createPlayer(@NotNull Motivator motivator) {
        Objects.requireNonNull(motivator)
        CharacterTrait.super.createPlayer(motivator)
    }

    @Override
    String toString() {
        "PlayerTemplate: ${persona.toString()}, nick ${nickName}, full ${fullName}, room ${room.id}, qty ${quantity.toString()}"
    }
}

package org.patdouble.adventuregame.model

import groovy.transform.Canonical

/**
 * Template for players in the story. Players can either be human or AI.
 */
@Canonical
class PlayerTemplate implements CharacterTrait {
    /** The allowed quantity of this type of player. */
    Range<Integer> quantity = 1..1
}

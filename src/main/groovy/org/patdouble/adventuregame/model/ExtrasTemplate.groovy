package org.patdouble.adventuregame.model

import groovy.transform.Canonical

/**
 * Template for extras in the story. Extras are always controlled by AI. An extra may be an important role, such as a
 * villain to defeat.
 */
@Canonical
class ExtrasTemplate implements CharacterTrait {
    /** The number of extras. */
    int quantity = 1
}

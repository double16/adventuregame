package org.patdouble.adventuregame.state

/**
 * The state of a story. The world, characters, positions, history, ...
 *
 * This is the top-level state object.
 */
class Story {
    List<Player> cast = new ArrayList<>()
    Stage stage
    History history
}

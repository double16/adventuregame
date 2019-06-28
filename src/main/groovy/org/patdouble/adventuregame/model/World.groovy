package org.patdouble.adventuregame.model

/**
 * Models the world for the starting point of one or more stories.
 *
 * This is the top level model object.
 */
class World {
    String name
    String author
    String description

    final List<Persona> personas = []

    /**
     * List of available players.
     */
    final List<PlayerTemplate> players = []

    /**
     * List of extras, which are like players but are only controlled by AI.
     */
    final List<ExtrasTemplate> extras = []

    final List<Room> rooms = []

    final List<Challenge> challenges = []

    Optional<Room> findRoomByName(String name) {
        getRooms().stream().filter { it.name == name }.findFirst()
    }
}

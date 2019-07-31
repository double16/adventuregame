package org.patdouble.adventuregame.model

import javax.validation.constraints.NotNull

/**
 * A place of non-deterministic size that can hold objects and players.
 */
class Room {
    String id
    String name
    String description

    /** Maps a direction to a room. */
    Map<String, Room> neighbors = new HashMap<>()

    Map<String, Room> getNeighbors() {
        Collections.unmodifiableMap(neighbors)
    }

    void addNeighbor(@NotNull String direction, @NotNull Room room) {
        assert direction != null
        assert room != null
        direction = direction.toLowerCase()
        if (neighbors.containsKey(direction)) {
            throw new IllegalArgumentException("Neighbor in direction ${direction} already present: ${neighbors.get(direction)}")
        }
        neighbors.put(direction, room)
    }
}

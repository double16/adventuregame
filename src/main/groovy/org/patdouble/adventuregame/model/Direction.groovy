package org.patdouble.adventuregame.model

import groovy.transform.CompileStatic

/**
 * Common directions used to describe relationships between rooms. The relationship is stored as a String and is not
 * restricted to the values here.
 */
@CompileStatic
enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    UP,
    DOWN

    static Optional<Direction> opposite(String direction) {
        if (direction == null) {
            return Optional.empty()
        }
        try {
            switch (valueOf(direction.toUpperCase())) {
                case NORTH: return Optional.of(SOUTH)
                case SOUTH: return Optional.of(NORTH)
                case EAST: return Optional.of(WEST)
                case WEST: return Optional.of(EAST)
                case UP: return Optional.of(DOWN)
                case DOWN: return Optional.of(UP)
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty()
        }
        return Optional.empty()
    }
}

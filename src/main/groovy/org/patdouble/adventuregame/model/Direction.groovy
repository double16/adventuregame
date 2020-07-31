package org.patdouble.adventuregame.model

import groovy.transform.CompileStatic

/**
 * Common directions used to describe relationships between rooms. The relationship is stored as a String and is not
 * restricted to the values here.
 */
@CompileStatic
enum Direction {
    NORTH,
    NORTHEAST,
    NORTHWEST,
    SOUTH,
    SOUTHEAST,
    SOUTHWEST,
    EAST,
    WEST,
    UP,
    DOWN

    @SuppressWarnings('EmptyCatchBlock')
    static Optional<Direction> opposite(String direction) {
        if (direction == null) {
            return Optional.empty()
        }
        try {
            switch (valueOf(direction.toUpperCase())) {
                case NORTH: return Optional.of(SOUTH)
                case NORTHEAST: return Optional.of(SOUTHWEST)
                case NORTHWEST: return Optional.of(SOUTHEAST)
                case SOUTH: return Optional.of(NORTH)
                case SOUTHEAST: return Optional.of(NORTHWEST)
                case SOUTHWEST: return Optional.of(NORTHEAST)
                case EAST: return Optional.of(WEST)
                case WEST: return Optional.of(EAST)
                case UP: return Optional.of(DOWN)
                case DOWN: return Optional.of(UP)
            }
        } catch (IllegalArgumentException e) {
            // fall thru expected
        }
        return Optional.empty()
    }
}

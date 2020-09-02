package org.patdouble.adventuregame.validation

import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World

/**
 * Finds disconnected sub-graphs of rooms (islands).
 */
@Slf4j
class IslandFinder {
    World world

    IslandFinder(World world) {
        this.world = world
    }

    Collection<Collection<Room>> computeIslands() {
        List<Collection<Room>> islands = new ArrayList<>()
        Set<Room> visited = new HashSet<>()
        for(Room room : world.rooms) {
            log.debug 'Creating island starting at room {}', room.modelId
            if (visited.contains(room)) {
                log.debug 'Room {} already visited, skipping', room.modelId
                continue
            }
            // this is an undirected graph, we need to check if we missed a one direction neighbor
            Set<Room> island = islands.find { Collection<Room> i ->
                i.contains(room) || room.neighbors.values().find { i.contains(it) }
            }
            if (!island) {
                log.debug 'Creating new island starting at {}', room.modelId
                island = new HashSet<>()
                islands << island
            } else {
                log.debug 'Existing island found for {}', room.modelId
            }

            List<Room> stack = new ArrayList<>()
            stack << room
            while (!stack.isEmpty()) {
                Room current = stack.pop()
                log.debug 'Adding room {} to island', room.modelId
                visited << current
                island << current
                stack.addAll(current.neighbors.values().findAll {n -> !visited.contains(n) })
                log.debug 'Neighbors left to scan: {}', stack
            }
        }

        islands
    }
}

package org.patdouble.adventuregame.validation

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World

/**
 * Finds disconnected sub-graphs of rooms (islands).
 */
@Slf4j
@CompileStatic
class IslandFinder {
    World world

    IslandFinder(World world) {
        this.world = world
    }

    Collection<Collection<Room>> computeIslands() {
        List<Collection<Room>> islands = []
        Set<Room> visited = [] as Set
        for (Room room : world.rooms) {
            log.debug 'Creating island starting at room {}', room.modelId
            if (visited.contains(room)) {
                log.debug 'Room {} already visited, skipping', room.modelId
                continue
            }
            // this is an undirected graph, we need to check if we missed a one direction neighbor
            Collection<Room> island = islands.find { Collection<Room> i ->
                i.contains(room) || room.neighbors.values().find { i.contains(it) }
            }
            if (island) {
                log.debug 'Existing island found for {}', room.modelId
            } else {
                log.debug 'Creating new island starting at {}', room.modelId
                island = [] as Set
                islands << island
            }

            List<Room> stack = []
            stack << room
            while (!stack.empty) {
                Room current = stack.pop()
                log.debug 'Adding room {} to island', room.modelId
                visited << current
                island << current
                stack.addAll(current.neighbors.values().findAll { n -> !visited.contains(n) })
                log.debug 'Neighbors left to scan: {}', stack
            }
        }

        islands
    }
}

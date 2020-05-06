package org.patdouble.adventuregame.engine.action

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.EngineFacade
import org.patdouble.adventuregame.flow.MapMessage
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.Player

import java.util.concurrent.CompletableFuture

/**
 * Implements {@link org.patdouble.adventuregame.model.Action#MAP}.
 */
@CompileStatic
class ActionMap implements ActionExecutor {
    @Override
    boolean execute(EngineFacade engine, Player player, ActionStatement action) {
        engine.findRoomsKnownToPlayer(player).thenAccept { Collection<Room> rooms ->
            MapMessage message = new MapMessage(player: player)
            rooms.each { Room from ->
                message.rooms.add(new MapMessage.RoomInfo(id: from.modelId, name: from.name))
                from.neighbors.each { String direction, Room to ->
                    if (rooms.contains(to)) {
                        message.edges.add(new MapMessage.RoomEdgeInfo(from: from.modelId, to: to.modelId, direction: direction))
                    }
                }
            }
            engine.submit(message)
        }
        true
    }
}

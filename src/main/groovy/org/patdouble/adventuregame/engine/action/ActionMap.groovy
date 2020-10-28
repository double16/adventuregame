package org.patdouble.adventuregame.engine.action

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.engine.EngineFacade
import org.patdouble.adventuregame.flow.MapMessage
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.model.Region
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.Player

/**
 * Implements {@link org.patdouble.adventuregame.model.Action#MAP}.
 */
@Slf4j
@CompileDynamic
class ActionMap implements ActionExecutor {
    @Override
    boolean execute(EngineFacade engine, Player player, ActionStatement action) {
        engine.findRoomsKnownToPlayer(player).thenAccept { Collection<Room> rooms ->
            Set<String> knownRegions = [] as Set
            MapMessage message = new MapMessage(player: player)
            rooms.each { Room from ->
                if (from.region) {
                    knownRegions << from.region.modelId
                }
                message.rooms.add(new MapMessage.RoomInfo(
                    id: from.modelId,
                    name: from.name,
                    region: from.region?.modelId))
                from.neighbors.each { String direction, Room to ->
                    if (rooms.contains(to)) {
                        String back = to.neighbors.entrySet().find { it.value.is(from) }?.key
                        message.edges.add(new MapMessage.RoomEdgeInfo(
                                from: from.modelId,
                                to: to.modelId,
                                direction: direction,
                                back: back))
                    }
                }
            }

            // Include the parents of the known regions
            knownRegions.asImmutable()
                .collect { engine.world.findRegionById(it as String).get().parent }
                .each { Region r ->
                    while (r != null) {
                        knownRegions << r.modelId
                        r = r.parent
                    }
                }
            for(String modelId : knownRegions) {
                Region r = engine.world.findRegionById(modelId).get()
                message.regions << new MapMessage.RegionInfo(r.modelId, r.name, r.parent?.modelId as String)
            }

            engine.submit(message)
        }.exceptionally { log.error("Creating map for player ${player}", it) }
        true
    }

    @Override
    boolean isValid(EngineFacade engine, Player player) {
        true
    }
}

package org.patdouble.adventuregame.engine.action

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.engine.EngineFacade
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

/**
 * Implements {@link org.patdouble.adventuregame.model.Action#GO}.
 */
@CompileDynamic
class ActionGo implements ActionExecutor {
    @Override
    boolean execute(EngineFacade engine, Player player, ActionStatement action) {
        String direction = action.directObject?.toLowerCase()
        List<String> directions = player.room.neighbors.keySet().sort()

        // match direction
        List<String> candidates
        if (!direction) {
            candidates = Collections.emptyList()
        } else if (directions.contains(direction)) {
            candidates = Collections.singletonList(direction)
        } else {
            candidates = directions.findAll { it.startsWith(direction) }
        }

        if (candidates.size() != 1) {
            Objects.requireNonNull(player.id)
            engine.submit(new PlayerNotification(player,
                    engine.bundles.text.getString('action.go.instructions.subject'),
                    engine.bundles.goInstructionsTemplate.make([directions: directions]).toString()))
            return false
        }

        player.room = player.room.neighbors.get(candidates.first())
        engine.updateObject(player, ['room'])

        true
    }

    @Override
    boolean isValid(EngineFacade engine, Player player) {
        !player.room.neighbors.empty
    }
}

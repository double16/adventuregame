package org.patdouble.adventuregame.engine.action

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.EngineFacade
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

/**
 * Implements one action.
 */
@CompileStatic
interface ActionExecutor {
    /**
     * Execute the action.
     * @param player the player to modify
     * @return true if the action was applied
     */
    boolean execute(EngineFacade engine, Player player, ActionStatement action)

    /**
     * Is the action valid for the current state of the player.
     * @return true if valid
     */
    boolean isValid(EngineFacade engine, Player player)
}

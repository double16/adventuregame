package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
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
}

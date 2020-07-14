package org.patdouble.adventuregame.engine.action

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.EngineFacade
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

/**
 * Implements {@link org.patdouble.adventuregame.model.Action#WAIT}.
 */
@CompileStatic
class ActionWait implements ActionExecutor {
    @Override
    boolean execute(EngineFacade engine, Player player, ActionStatement action) {
        true
    }

    @Override
    boolean isValid(EngineFacade engine, Player player) {
        true
    }
}

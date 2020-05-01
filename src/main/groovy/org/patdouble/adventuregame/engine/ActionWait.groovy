package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
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
}

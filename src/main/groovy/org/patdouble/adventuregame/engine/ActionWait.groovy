package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

@CompileStatic
class ActionWait implements ActionExecutor {
    @Override
    boolean execute(EngineFacade engine, Player player, ActionStatement action) {
        true
    }
}

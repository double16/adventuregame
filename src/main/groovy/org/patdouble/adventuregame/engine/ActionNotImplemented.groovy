package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

@CompileStatic
class ActionNotImplemented implements ActionExecutor {
    @Override
    boolean execute(EngineFacade engine, Player player, ActionStatement action) {
        throw new UnsupportedOperationException('Not implemented')
    }
}

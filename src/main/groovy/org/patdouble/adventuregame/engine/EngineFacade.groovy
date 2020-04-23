package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

/**
 * Exposes a subset of methods from {@link Engine} to the rules engine.
 */
@CompileStatic
class EngineFacade {
    protected final Engine engine
    EngineFacade(Engine engine) {
        this.engine = engine
    }

    /**
     * {@link Engine#createActionRequest(org.patdouble.adventuregame.state.Player)}
     */
    @SuppressWarnings(['Unused', 'BuilderMethodWithSideEffects'])
    void createActionRequest(Player player) {
        engine.createActionRequest(player)
    }

    /**
     * {@link Engine#incrementChronos()}
     */
    @SuppressWarnings('Unused')
    void incrementChronos() {
        engine.incrementChronos()
    }

    /**
     * {@link Engine#action(org.patdouble.adventuregame.state.Player, org.patdouble.adventuregame.i18n.ActionStatement)}
     */
    @SuppressWarnings('Unused')
    void action(Player player, String verb, String directObject, String indirectObject) {
        ActionStatement stmt = new ActionStatement(
                verb: verb,
                directObject: directObject,
                indirectObject: indirectObject)
        engine.action(player, stmt)
    }

    /**
     * {@link Engine#end()}
     */
    @SuppressWarnings('Unused')
    void end() {
        engine.end()
    }
}

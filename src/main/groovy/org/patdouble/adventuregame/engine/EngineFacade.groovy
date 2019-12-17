package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

/**
 * Exposes a subset of methods from {@link Engine} to the rules engine.
 */
class EngineFacade {
    private final Engine delegate
    EngineFacade(Engine engine) {
        this.delegate = engine
    }

    /**
     * {@link Engine#createActionRequest(org.patdouble.adventuregame.state.Player)}
     */
    @SuppressWarnings("Unused")
    void createActionRequest(Player player) {
        delegate.createActionRequest(player)
    }

    /**
     * {@link Engine#incrementChronos()}
     */
    @SuppressWarnings("Unused")
    void incrementChronos() {
        delegate.incrementChronos()
    }

    /**
     * {@link Engine#action(org.patdouble.adventuregame.state.Player, org.patdouble.adventuregame.i18n.ActionStatement)}
     */
    @SuppressWarnings("Unused")
    void action(Player player, String verb, String directObject, String indirectObject) {
        delegate.action(player, new ActionStatement(verb: verb, directObject: directObject, indirectObject: indirectObject))
    }

    /**
     * {@link Engine#close()}
     */
    @SuppressWarnings("Unused")
    void close() {
        delegate.close()
    }
}

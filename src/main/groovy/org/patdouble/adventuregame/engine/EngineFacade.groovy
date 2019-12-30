package org.patdouble.adventuregame.engine

import org.kie.api.runtime.rule.RuleRuntime
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.state.Player

/**
 * Exposes a subset of methods from {@link Engine} to the rules engine.
 */
class EngineFacade {
    protected final Engine engine
    EngineFacade(Engine engine) {
        this.engine = engine
    }

    /**
     * {@link Engine#createActionRequest(org.patdouble.adventuregame.state.Player)}
     */
    @SuppressWarnings("Unused")
    void createActionRequest(Player player) {
        engine.createActionRequest(player)
    }

    /**
     * {@link Engine#incrementChronos()}
     */
    @SuppressWarnings("Unused")
    void incrementChronos() {
        engine.incrementChronos()
    }

    /**
     * {@link Engine#action(org.patdouble.adventuregame.state.Player, org.patdouble.adventuregame.i18n.ActionStatement)}
     */
    @SuppressWarnings("Unused")
    void action(Player player, String verb, String directObject, String indirectObject) {
        engine.action(player, new ActionStatement(verb: verb, directObject: directObject, indirectObject: indirectObject))
    }

    /**
     * {@link Engine#close(org.kie.api.runtime.rule.RuleRuntime)}
     */
    @SuppressWarnings("Unused")
    void close(RuleRuntime ruleRuntime) {
        engine.close(ruleRuntime)
    }
}
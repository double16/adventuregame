package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
import org.kie.api.runtime.KieSession
import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.i18n.Bundles
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.GoalStatus
import org.patdouble.adventuregame.state.Player

import java.util.concurrent.CompletableFuture

/**
 * Exposes a subset of methods from {@link Engine} to the rules engine.
 */
@CompileStatic
class EngineFacade {
    protected final Engine engine
    protected final ThreadLocal<KieSession> kieSession = new ThreadLocal<>()

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

    /**
     * Publish a message.
     * @param storyMessage
     * @return the estimated maximum lag among subscribers
     */
    int submit(StoryMessage item) {
        engine.submit(item)
    }

    Bundles getBundles() {
        engine.bundles
    }

    /**
     * Mark a goal as fulfilled.
     */
    @SuppressWarnings('Unused')
    void fulfill(GoalStatus goal) {
        engine.fulfill(goal)
    }

    /**
     * Get a list of rooms known to the player.
     */
    @SuppressWarnings('Unused')
    CompletableFuture<Collection<Room>> findRoomsKnownToPlayer(Player p) {
        engine.findRoomsKnownToPlayer(p)
    }

    /**
     * Update an object in the rule engine. Only to be called while running a rule consequence.
     * @param object the object, such as Player
     * @param changed list of changed properties, null to attempt detection, 0 implies immutable so no change is recorded
     */
    void updateObject(Object object, Collection<String> changed = null) {
        engine.addOrReplaceKieObject(kieSession.get(), object, changed as String[])
    }
}

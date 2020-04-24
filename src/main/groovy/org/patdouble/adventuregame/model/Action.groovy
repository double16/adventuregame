package org.patdouble.adventuregame.model

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.ActionExecutor
import org.patdouble.adventuregame.engine.ActionGo
import org.patdouble.adventuregame.engine.ActionNotImplemented
import org.patdouble.adventuregame.engine.ActionWait

/**
 * Recommended actions. Any verb can be used but these may come with built in synonyms.
 */
@CompileStatic
enum Action {
    GO(1, ActionGo),
    /** Skip an action for this turn. There is a minor health benefit, unless the player is in adverse conditions. */
    WAIT(1, ActionWait),
    PAY(1, ActionNotImplemented),
    FIGHT(1, ActionNotImplemented),
    LOOK(0, ActionNotImplemented),
    SAY(0, ActionNotImplemented),
    TAKE(1, ActionNotImplemented),
    DROP(1, ActionNotImplemented),
    /**
     * Sleep is used to restore health by skipping actions. It is similar to wait but takes either a chronos duration
     * or desired health value before reporting surroundings or requesting action. Actions or other events can awake
     * a player. This does leave a player vulnerable.
     */
    SLEEP(1, ActionNotImplemented),
    /** Wake a sleeping player. */
    WAKE(0, ActionNotImplemented),
    /** Capture a player. */
    CAPTURE(1, ActionNotImplemented),
    /** Release a player. */
    RELEASE(1, ActionNotImplemented)

    /** How many time units does this action take? */
    final long chronosCost
    final Class<? extends ActionExecutor> actionClass

    Action(long chronosCost, Class<? extends ActionExecutor> actionClass) {
        this.chronosCost = chronosCost
        this.actionClass = actionClass
    }
}

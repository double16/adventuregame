package org.patdouble.adventuregame.model

import groovy.transform.CompileStatic

/**
 * Recommended actions. Any verb can be used but these may come with built in synonyms.
 */
@CompileStatic
enum Action {
    GO(1),
    /** Skip an action for this turn. There is a minor health benefit, unless the player is in adverse conditions. */
    WAIT(1),
    PAY(1),
    FIGHT(1),
    LOOK(0),
    SAY(0),
    TAKE(1),
    DROP(1),
    /**
     * Sleep is used to restore health by skipping actions. It is similar to wait but takes either a chronos duration
     * or desired health value before reporting surroundings or requesting action. Actions or other events can awake
     * a player. This does leave a player vulnerable.
     */
    SLEEP(1),
    /** Wake a sleeping player. */
    WAKE(0)

    /** How many time units does this action take? */
    final long chronosCost

    Action(long chronosCost) {
        this.chronosCost = chronosCost
    }
}

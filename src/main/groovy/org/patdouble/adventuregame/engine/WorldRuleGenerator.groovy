package org.patdouble.adventuregame.engine

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.World

/**
 * Generates the Drools rules for a World.
 */
@CompileDynamic
class WorldRuleGenerator {
    private static final String PROLOGUE = """
package org.patdouble.adventuregame.state

dialect "mvel"

global org.slf4j.Logger log;
global org.patdouble.adventuregame.engine.EngineFacade engine;
"""

    private final World world

    WorldRuleGenerator(World world) {
        this.world = world
    }

    /**
     * Generate the DRL and DSLR documents into the writers. The entire documents are expected to be generated,
     * including package spec, dialect, etc.
     */
    void generate(Writer drl, Writer dslr) {
        drl.append(PROLOGUE)
        dslr.append(PROLOGUE)

        world.goals.findAll { it.rules }.each { Goal g ->
            dslr.append("""

rule "world goal ${g.name}"
  when
    Story Goal "${g.name}"
    ${g.rules.join('\n')}
  then
    log "goal ${g.name} fulfilled"
    goal is fulfilled
end

""")
        }
    }
}

package org.patdouble.adventuregame.ui.console

import org.fusesource.jansi.Ansi
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Story

class Main {
    /** Used to stop the main thread because this is a reactive application. */
    private static final Object WAIT_LOCK = new Object()

    static void main(String[] args) {
        final Console console = new Console()

        UniverseRegistry registry = new UniverseRegistry()
        World world = registry.getWorlds().find { it.name == 'Trailer Park' }

        console.println {
            newline()
            .a(Ansi.Attribute.NEGATIVE_ON).a("Welcome to ${world.name}").a(Ansi.Attribute.NEGATIVE_OFF)
            .newline()
        }

        Story story = new Story(world)
        Engine engine = new Engine(story)
        engine.autoLifecycle = true
        engine.subscribe(new ConsoleRequestHandler(console, engine))
//        engine.subscribe(new StoryMessageOutput())
        engine.init()

        synchronized (WAIT_LOCK) {
            WAIT_LOCK.wait()
        }
    }
}

package org.patdouble.adventuregame.ui.console

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import groovy.transform.CompileDynamic
import org.fusesource.jansi.Ansi
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.flow.EngineCloseOnStoryEnd
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.state.Story
import org.slf4j.LoggerFactory

/**
 * Console UI for story engine.
 */
@CompileDynamic
class Main {
    /** Used to stop the main thread because this is a reactive application. */
    private static final Object WAIT_LOCK = new Object()

    static void main(String[] args) {
        Level logLevel = Level.ERROR
        for (String arg : args) {
            switch (arg) {
                case '--info':
                    logLevel = Level.INFO
                    break
                case '--debug':
                    logLevel = Level.DEBUG
                    break
            }
        }
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(logLevel)
        ((Logger) LoggerFactory.getLogger('org.appformer.maven')).setLevel(Level.OFF)

        final Console CONSOLE = new Console()

        UniverseRegistry registry = new YamlUniverseRegistry()
        World world = registry.worlds.find { it.name == YamlUniverseRegistry.TRAILER_PARK }

        CONSOLE.println {
            newline()
            .a(Ansi.Attribute.NEGATIVE_ON).a("Welcome to ${world.name}").a(Ansi.Attribute.NEGATIVE_OFF)
            .newline()
        }

        Story story = new Story(world)
        Engine engine = new Engine(story)
        engine.autoLifecycle = true
        engine.subscribe(new ConsoleRequestHandler(CONSOLE, engine))
        engine.subscribe(new EngineCloseOnStoryEnd(engine))
//        engine.subscribe(new StoryMessageOutput())
        engine.init()

        while (!engine.closed) {
            synchronized (WAIT_LOCK) {
                WAIT_LOCK.wait()
            }
        }
    }
}

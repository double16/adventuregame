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

    Level logLevel = Level.ERROR
    Console console
    UniverseRegistry registry
    World world
    Story story
    Engine engine

    static void main(String[] args) {
        Main main = new Main()
        main.configureViaCommandLine(args)
        main.applyConfiguration()
        main.chooseWorld()
        main.start()

        while (!main.engine.closed) {
            synchronized (WAIT_LOCK) {
                WAIT_LOCK.wait()
            }
        }
    }

    void configureViaCommandLine(String[] args) {
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
    }

    void applyConfiguration() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(logLevel)
        ((Logger) LoggerFactory.getLogger('org.appformer.maven')).setLevel(Level.OFF)
        if (console == null) {
            console = new Console()
        }
        if (registry == null) {
            registry = new YamlUniverseRegistry()
        }
    }

    void chooseWorld() {
        world = registry.worlds.find { it.name == YamlUniverseRegistry.TRAILER_PARK }
    }

    void start(Closure exitStrategy = null) {
        console.println {
            newline()
                    .a(Ansi.Attribute.NEGATIVE_ON).a("Welcome to ${world.name}").a(Ansi.Attribute.NEGATIVE_OFF)
                    .newline()
        }

        story = new Story(world)
        engine = new Engine(story)
        engine.autoLifecycle = true
        engine.subscribe(new ConsoleRequestHandler(console, engine, exitStrategy))
        engine.subscribe(new EngineCloseOnStoryEnd(engine))
//        engine.subscribe(new StoryMessageOutput())
        engine.init()
    }
}

package org.patdouble.adventuregame

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import groovyx.gpars.actor.Actors
import org.fusesource.jansi.Ansi
import org.patdouble.adventuregame.engine.DroolsConfiguration
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.model.Region
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.UniverseRegistry
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.lua.WorldLuaStorage
import org.patdouble.adventuregame.ui.console.Console

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Validates a world definition.
 */
@CompileDynamic
class WorldValidator {
    private static final DIRECTION_TO_NODEPORT = [
            'north': 's',
            'northeast': 'sw',
            'northwest': 'se',
            'south': 'n',
            'southeast': 'nw',
            'southwest': 'ne',
            'east': 'w',
            'west': 'e',
    ]

    @CompileStatic
    @Canonical
    static class GoalTrial {
        World world
    }

    Console console
    UniverseRegistry universeRegistry
    DroolsConfiguration droolsConfiguration = new DroolsConfiguration()
    /** The number of runs to determine goal outcomes and performance. */
    int aiRunCount = 100

    WorldValidator(Console console, UniverseRegistry universeRegistry) {
        this.console = console
        this.universeRegistry = universeRegistry
    }

    private Console printResult(boolean result) {
        console.print {
            if (result) {
                fg(Ansi.Color.GREEN).a('OK').fg(Ansi.Color.DEFAULT)
            } else {
                fg(Ansi.Color.RED).a('FAIL').fg(Ansi.Color.DEFAULT)
            }
        }
    }

    private static CharSequence gvQuotedString(CharSequence s) {
        "\"${s.replaceAll('"', '\\""')}\""
    }

    private static String directionToNodePort(String direction) {
        String port = DIRECTION_TO_NODEPORT[direction]
        port ? ":${port}" : ''
    }

    private void mapSubgraph(Writer out, String indent, World world, Region region) {
        if (region != null) {
            out << """${indent}subgraph cluster_${region.modelId} {
${indent}  label=${gvQuotedString(region.name)};
"""
        }
        world.regions.findAll { it.parent == region }.each { mapSubgraph(out, indent+'  ', world, it) }

        world.rooms
            .findAll { it.region == region }
            .each { out << "${indent}  ${it.modelId}[label=${gvQuotedString(it.name)}];\n" }
            .join(';')

        if (region != null) {
            out << "${indent}}\n"
        }
    }

    void help() {
        console.println "${WorldValidator.class.simpleName} [-r|--runs n] <world_name|world_uuid|file_location>"
        System.exit(1)
    }

    World loadWorld(String worldSpec) {
        World world = universeRegistry.worlds.find {
            it.id?.toString()?.equalsIgnoreCase(worldSpec) || it.name.equalsIgnoreCase(worldSpec)
        }
        if (!world) {
            File f = new File(worldSpec)
            if (!f.exists() || !f.isFile() || !f.canRead()) {
                console.println "${worldSpec}: file not found"
                System.exit(2)
            }
            f.withInputStream {
                world = new WorldLuaStorage().load(it)
            }
        }
        world
    }

    boolean compile(World world) {
        console.print 'Compiling... '
        boolean result = droolsConfiguration.kieContainer(world)
        printResult(result).println()
        result
    }

    File map(World world) {
        File f = new File("${world.name}_${world.computeSecureHash()}.dot")
        console.print 'Generating map ... '
        f.withWriter {
            it << """
digraph {
  label=${gvQuotedString(world.name)};
  concentrate=true;
"""

            mapSubgraph(it, '', world, null)
            world.rooms.each { Room from ->
                from.neighbors.each { String direction, Room to ->
                    String returnDirection = to.neighbors.find { k,v -> v == from }?.key
                    it << """  ${from.modelId}${directionToNodePort(returnDirection)} -> ${to.modelId}${directionToNodePort(direction)}[label=${gvQuotedString(direction)}];\n"""
                }
            }
            it << """}
"""
        }
        console.println f.name
        f
    }

    void goalPerformance(World world) {
        console.println('Determining goal probability ...')
        List<GoalTrial> trials = [new GoalTrial(world)] * aiRunCount
        AtomicInteger completeCount = new AtomicInteger(0)
        AtomicInteger timeoutCount = new AtomicInteger(0)
        List<Exception> errors = [].asSynchronized()
        ConcurrentHashMap<String, AtomicInteger> goalFulfilledCounts = new ConcurrentHashMap<>()
        GParsPool.withPool {
            def reporter = Actors.actor {
                boolean first = true
                loop {
                    react {
                        Story story = it[0]
                        int num = it[1]
                        console.print {
                            if (!first) {
                                cursorUpLine(story.goals.size() + 2)
                            } else {
                                first = false
                            }

                            int statColumn = Math.max(10, story.goals.collect { 8+it.goal.name.length() }.max() ?: 0)

                            eraseLine()
                            render('Progress:')
                            cursorToColumn(statColumn)

                            render('%d/%d', num, trials.size())
                            if (!errors.empty || timeoutCount.get() > 0) {
                                fg(Ansi.Color.RED)
                                render(', %d errors, %d timed out', errors.size(), timeoutCount.get())
                                fg(Ansi.Color.DEFAULT)
                            }
                            newline()

                            story.goals.each {
                                eraseLine()
                                render('Goal %s:', it.goal.name)
                                cursorToColumn(statColumn)
                                render('%d/%d', goalFulfilledCounts.get(it.goal.name).intValue(), trials.size())
                                newline()
                            }

                            eraseLine()
                            render('Memory:')
                            cursorToColumn(statColumn)
                            render('%dM/%dM', (int) (Runtime.runtime.freeMemory()/(1024*1024)), (int) (Runtime.runtime.totalMemory()/(1024*1024)))
                            newline()

                            delegate
                        }
                        if (num == trials.size()) {
                            terminate()
                        }
                    }
                }
            }

            trials.eachParallel {
                Story story = null
                Engine engine = null
                try {
                    story = new Story(world)
                    engine = new Engine(story)
                    engine.autoLifecycle = true
                    engine.init().join()
                    engine.start(Motivator.AI).join()
                    CompletableFuture.anyOf(engine.storyEnd, engine.firingComplete).get(30, TimeUnit.SECONDS)
                    // prevent closing while processing the ending
                    engine.end().join()
                    engine.close().join()
                } catch (TimeoutException e) {
                    timeoutCount.incrementAndGet()
                } catch (Exception e) {
                    errors << e
                } finally {
                    try {
                        engine?.close()
                    } catch (Exception e) {
                        errors << e
                    }
                }

                int num = completeCount.incrementAndGet()
                if (story != null) {
                    story.goals.each {
                        AtomicInteger count = goalFulfilledCounts.computeIfAbsent(it.goal.name, { new AtomicInteger(0) })
                        if (it.fulfilled) {
                            count.incrementAndGet()
                        }
                    }
                    reporter << [story, num]
                }
            }

            reporter.join(30, TimeUnit.SECONDS)
            reporter.terminate()
        }

        errors*.printStackTrace()
    }

    boolean validate(World world) {
        console.println "Validating ${world.name}"
        boolean result = compile(world)
        summary(world)
        map(world)
        // TODO: Report 'islands'
        goalPerformance(world)
        printResult(result).println()
    }

    boolean summary(World world) {
        console.println "${world.regions.size()} regions, ${world.rooms.size()} rooms, ${world.personas.size()} personas, ${world.players.size()} players"
    }

    void run(List<String> args) {
        if (args.empty) {
            help()
        }

        List<String> worldSpecs = []
        List<String> toParse = args.clone()
        while (!toParse.empty) {
            String s = toParse.pop()
            if (s.startsWith('-')) {
                switch (s) {
                    case '-r':
                    case '--runs':
                        aiRunCount = toParse.pop() as int
                        break
                    default:
                        help()
                }
            } else {
                worldSpecs << s
            }
        }
        if (worldSpecs.empty) {
            help()
        }

        worldSpecs.each { String worldSpec ->
            World world = loadWorld(worldSpec)
            validate(world)
        }
    }
}

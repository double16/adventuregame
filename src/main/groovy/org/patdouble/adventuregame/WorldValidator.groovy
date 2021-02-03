package org.patdouble.adventuregame

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import org.apache.commons.io.output.CloseShieldOutputStream
import org.apache.commons.io.output.TeeOutputStream
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
import org.patdouble.adventuregame.validation.IslandFinder
import org.springframework.util.StreamUtils

import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Validates a world definition.
 */
@CompileDynamic
@SuppressWarnings(['SystemExit', 'FileCreateTempFile'])
class WorldValidator {
    static final String MAP_FILE_NAME = 'map'
    static final String GRAPHVIZ_FILE_EXT = '.dot'
    static final String SVG_FILE_EXT = '.svg'
    static final String RESULT_TEXT_SUCCESS = 'OK'
    static final String RESULT_TEXT_FAIL = 'FAIL'
    static final String RESULT_TEXT_WARN = 'WARN'

    private static final Map<String, String> DIRECTION_TO_NODEPORT = [
            'north'    : 's',
            'northeast': 'sw',
            'northwest': 'se',
            'south'    : 'n',
            'southeast': 'nw',
            'southwest': 'ne',
            'east'     : 'w',
            'west'     : 'e',
    ].asImmutable()

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()

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
                fg(Ansi.Color.GREEN).a(RESULT_TEXT_SUCCESS).fg(Ansi.Color.DEFAULT)
            } else {
                fg(Ansi.Color.RED).a(RESULT_TEXT_FAIL).fg(Ansi.Color.DEFAULT)
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
        world.regions.findAll { it.parent == region }.each { mapSubgraph(out, indent + '  ', world, it) }

        world.rooms
                .findAll { it.region == region }
                .each { out << "${indent}  ${it.modelId}[label=${gvQuotedString(it.name)}];\n" }
                .join(';')

        if (region != null) {
            out << "${indent}}\n"
        }
    }

    void help() {
        console.println "${WorldValidator.simpleName} [-r|--runs n] <world_name|world_uuid|file_location>"
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

    void map(World world, ZipOutputStream zip) {
        console.print 'Generating map ... '

        File dotFile = File.createTempFile(MAP_FILE_NAME, GRAPHVIZ_FILE_EXT)
        File svgFile = File.createTempFile(MAP_FILE_NAME, SVG_FILE_EXT)

        ZipEntry ze = new ZipEntry("${MAP_FILE_NAME}${GRAPHVIZ_FILE_EXT}")
        ze.comment = 'Map in GraphViz DOT format, http://graphviz.org'
        zip.putNextEntry(ze)
        OutputStreamWriter writer = new OutputStreamWriter(new TeeOutputStream(
                new CloseShieldOutputStream(zip),
                new FileOutputStream(dotFile)),
                Charset.forName('UTF-8'))
        try {
            writer << """
digraph {
  label=${gvQuotedString(world.name)};
  concentrate=true;
"""
            mapSubgraph(writer, '', world, null)
            Set<String> skipEdges = [] as Set
            world.rooms.each { Room from ->
                from.neighbors.each { String direction, Room to ->
                    String returnDirection = to.neighbors.find { k, v -> v == from }?.key
                    if (!returnDirection ||
                            !skipEdges.contains("${from.modelId}${returnDirection}:${to.modelId}${direction}" as String)) {
                        String label = direction
                        if (returnDirection) {
                            if (returnDirection < direction) {
                                label = "${returnDirection}/${direction}"
                            } else {
                                label = "${direction}/${returnDirection}"
                            }
                        }
                        writer << """  ${from.modelId}${directionToNodePort(returnDirection)} -> ${to.modelId}${directionToNodePort(direction)}[label=${gvQuotedString(label)}"""
                        if (returnDirection) {
                            writer << ',dir=none'
                        }
                        writer << '];\n'

                        if (returnDirection) {
                            skipEdges << ("${to.modelId}${direction}:${from.modelId}${returnDirection}" as String)
                        }
                    }
                }
            }
            writer << '''}
'''
        } finally {
            writer.close()
        }

        zip.closeEntry()
        console.print 'DOT '

        // Generate SVG
        if (new ProcessBuilder().command('dot', '-Tsvg', "-o${svgFile.absolutePath}", dotFile.absolutePath)
                .start().waitFor() == 0 && svgFile.length() > 0) {
            ze = new ZipEntry("${MAP_FILE_NAME}${SVG_FILE_EXT}")
            ze.comment = 'Map in SVG format (most web browsers support SVG)'
            zip.putNextEntry(ze)
            svgFile.withInputStream {
                StreamUtils.copy(it, new CloseShieldOutputStream(zip))
            }
            zip.closeEntry()
            console.print 'SVG '
        }

        dotFile.delete()
        svgFile.delete()

        printResult(true)
        console.println()
    }

    @SuppressWarnings('NestedBlockDepth')
    void goalPerformance(World world, ZipOutputStream zip) {
        console.println('Determining goal probability ...')
        List<GoalTrial> trials = [new GoalTrial(world)] * aiRunCount
        AtomicInteger completeCount = new AtomicInteger(0)
        AtomicInteger timeoutCount = new AtomicInteger(0)
        List<Exception> errors = [].asSynchronized()
        ConcurrentMap<String, AtomicInteger> goalFulfilledCounts = new ConcurrentHashMap<>()
        GParsPool.withPool {
            Actor reporter = Actors.actor {
                boolean first = true
                AtomicLong start = new AtomicLong()
                loop {
                    react {
                        Story story = it[0]
                        int num = it[1]
                        console.print {
                            if (first) {
                                start.set(System.currentTimeMillis())
                                first = false
                            } else {
                                cursorUpLine(story.goals.size() + 3)
                            }

                            int statColumn = Math.max(10, story.goals.collect { 8 + it.goal.name.length() }.max() ?: 0)

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

                            render('Elapsed:')
                            cursorToColumn(statColumn)
                            long millis = System.currentTimeMillis() - start.get()
                            int minutes = (int) (millis / 60000)
                            int seconds = (int) ((millis % 60000) / 1000)
                            render('%d:%02d', minutes, seconds)
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
                            render('%dM/%dM',
                                    (int) (Runtime.runtime.freeMemory() / (1024 * 1024)),
                                    (int) (Runtime.runtime.totalMemory() / (1024 * 1024)))
                            newline()

                            ZipEntry ze = new ZipEntry("history${num}.json")
                            ze.comment = "Goals met: ${story.goals.findAll { it.fulfilled }.collect { it.goal.name }}"
                            zip.putNextEntry(ze)
                            OBJECT_MAPPER.writeValue(new CloseShieldOutputStream(zip), story.history)
                            zip.closeEntry()

                            delegate
                        }
                        if (num == trials.size()) {
                            terminate()
                        }
                    }
                }
            }

            trials.eachParallel { GoalTrial trial ->
                Story story = null
                Engine engine = null
                try {
                    story = new Story(trial.world)
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
                        AtomicInteger count = goalFulfilledCounts.computeIfAbsent(it.goal.name) { new AtomicInteger(0) }
                        if (it.fulfilled) {
                            count.incrementAndGet()
                        }
                    }
                    reporter << [story, num]
                }
            }

            reporter.join(10 * trials.size(), TimeUnit.SECONDS)
            reporter.terminate()
        }

        ZipEntry ze = new ZipEntry('goal_perf.json')
        ze.comment = 'Goal Performance'
        zip.putNextEntry(ze)
        OBJECT_MAPPER.writeValue(new CloseShieldOutputStream(zip), [trials: trials.size(), goals: goalFulfilledCounts, errors: errors])
        zip.closeEntry()

        errors*.printStackTrace()
    }

    void findIslands(World world, ZipOutputStream zip) {
        Collection<Collection<Room>> islands = new IslandFinder(world).computeIslands()
        console.print "Island count: ${islands.size()}  "
        console.print {
            if (islands.size() == 1) {
                fg(Ansi.Color.GREEN).a(RESULT_TEXT_SUCCESS).fg(Ansi.Color.DEFAULT)
            } else {
                fg(Ansi.Color.YELLOW).a(RESULT_TEXT_WARN).fg(Ansi.Color.DEFAULT)
            }
        }
        console.println()

        if (islands.size() > 1) {
            ZipEntry ze = new ZipEntry('islands.json')
            ze.comment = 'Independent islands (room sub-graphs)'
            zip.putNextEntry(ze)
            Collection<List<String>> islandOutput = islands*.modelId
            OBJECT_MAPPER.writeValue(new CloseShieldOutputStream(zip), islandOutput)
            zip.closeEntry()
        }
    }

    boolean validate(World world) {
        console.println "Validating ${world.name}"
        File zipReportFile = new File("${world.name.replaceAll('\\s+', '_')}_${world.hash}.zip")
        zipReportFile.delete()
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipReportFile)))
        zip.comment = "${world.name}, ${world.hash}"
        boolean result = false
        try {
            result = compile(world)
            summary(world)
            findIslands(world, zip)
            map(world, zip)
            goalPerformance(world, zip)
            printResult(result).println()
        } finally {
            zip.close()
        }
        console.println zipReportFile
        result
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
            world.prePersist()
            validate(world)
        }
    }
}

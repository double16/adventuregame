package org.patdouble.adventuregame.engine

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.kie.api.KieBaseConfiguration
import org.kie.api.KieServices
import org.kie.api.builder.KieBuilder
import org.kie.api.builder.KieFileSystem
import org.kie.api.builder.KieModule
import org.kie.api.builder.Message
import org.kie.api.builder.Results
import org.kie.api.conf.EqualityBehaviorOption
import org.kie.api.runtime.KieContainer
import org.kie.internal.io.ResourceFactory
import org.patdouble.adventuregame.model.World
import org.slf4j.LoggerFactory

import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Configures the rule engine.
 */
@Slf4j
@CompileDynamic
class DroolsConfiguration {
    private static final List<String> RULE_FILES = [
            'org/patdouble/adventuregame/state/default.dsl',
            'org/patdouble/adventuregame/state/default.dslr',
            'org/patdouble/adventuregame/state/default.drl',
    ]

    private static final ConcurrentHashMap<String, SoftReference<KieContainer>> CACHE = new ConcurrentHashMap<>()

    /**
     * Create a new KIE container with custom rules generated from the World.
     * @param worldRulesDrl rules in DRL format
     * @param worldRulesDslr rules in DSLR format
     */
    KieContainer kieContainer(World world) {
        KieContainer container = CACHE.compute(world.hash ?: world.computeSecureHash(),
                { String key, SoftReference<KieContainer> value ->
                    if (value?.get()) {
                        return value
                    }
                    if (value != null) {
                        log.info 'Rebuilding gc\'d KIE container'
                    }
                    new SoftReference<>(buildContainer(world))
                }).get()
        Objects.requireNonNull(container)
        container
    }

    @SuppressWarnings(['UnnecessaryGetter', 'UnnecessarySetter'])
    private KieContainer buildContainer(World world) {
        log.info 'Building KIE container for {} - {} #{}', world.id, world.name, world.edition

        final int INITIAL_BUFFER_SIZE = 16384
        WorldRuleGenerator worldRuleGenerator = new WorldRuleGenerator(world)
        StringWriter worldRulesDrl = new StringWriter(INITIAL_BUFFER_SIZE)
        StringWriter worldRulesDslr = new StringWriter(INITIAL_BUFFER_SIZE)
        worldRuleGenerator.generate(worldRulesDrl, worldRulesDslr)

        KieServices kieServices = KieServices.Factory.get()
        Objects.requireNonNull(kieServices, 'could not load factory KieServices')

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem()
        RULE_FILES.each {
            kieFileSystem.write(ResourceFactory.newClassPathResource(it))
        }
        if (LoggerFactory.getLogger('org.patdouble.adventuregame.engine.Engine').isDebugEnabled()) {
            kieFileSystem.write(ResourceFactory.newClassPathResource('org/patdouble/adventuregame/state/debug.drl'))
        }
        if (worldRulesDrl) {
            kieFileSystem.write('src/main/resources/org/patdouble/adventuregame/state/world.drl', worldRulesDrl.toString())
        }
        if (worldRulesDslr) {
            kieFileSystem.write('src/main/resources/org/patdouble/adventuregame/state/world.dslr', worldRulesDslr.toString())
        }
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem)

        Results results = kieBuilder.buildAll().results
        StringBuilder exceptionMessage = new StringBuilder()
        results.messages.each { Message m ->
            String logText = 'Building rule engine for {}: {} {}@{},{} {}'
            Object[] logArgs = [world.name, m.id, m.path, m.line, m.column, m.text] as Object[]

            switch (m.level) {
                case Message.Level.ERROR:
                    //log.error(logText, logArgs)
                    if (exceptionMessage.length() > 0) {
                        exceptionMessage.append('\n')
                    }
                    exceptionMessage.append(m.toString())
                    break
                case Message.Level.WARNING:
                    log.warn(logText, logArgs)
                    break
                case Message.Level.INFO:
                    log.info(logText, logArgs)
                    break
            }
        }
        if (exceptionMessage.length() > 0) {
            throw new RuntimeException(exceptionMessage.toString())
        }

        KieModule kieModule = kieBuilder.getKieModule()

        KieContainer kContainer = kieServices.newKieContainer(kieModule.getReleaseId())

        KieBaseConfiguration kieBaseConfiguration = kieServices.newKieBaseConfiguration()
        kieBaseConfiguration.setOption(EqualityBehaviorOption.EQUALITY)

        kContainer.newKieBase(kieBaseConfiguration)
        log.info 'Built KIE container for {} - {} #{}', world.id, world.name, world.edition
        kContainer
    }
}

package org.patdouble.adventuregame.engine

import groovy.transform.CompileDynamic
import org.kie.api.KieBaseConfiguration
import org.kie.api.KieServices
import org.kie.api.builder.KieBuilder
import org.kie.api.builder.KieFileSystem
import org.kie.api.builder.KieModule
import org.kie.api.conf.EqualityBehaviorOption
import org.kie.api.runtime.KieContainer
import org.kie.internal.io.ResourceFactory

/**
 * Configures the rule engine.
 */
@CompileDynamic
class DroolsConfiguration {
    private static final List<String> RULE_FILES = [
            'org/patdouble/adventuregame/state/default.dsl',
            'org/patdouble/adventuregame/state/default.dslr',
            'org/patdouble/adventuregame/state/debug.drl',
            'org/patdouble/adventuregame/state/default.drl',
    ]

    /**
     * Create a new KIE container with custom rules generated from the World.
     * @param worldRulesDrl rules in DRL format
     * @param worldRulesDslr rules in DSLR format
     */
    @SuppressWarnings(['UnnecessaryGetter', 'UnnecessarySetter'])
    KieContainer kieContainer(String worldRulesDrl = null, String worldRulesDslr = null) {
        KieServices kieServices = KieServices.Factory.get()
        Objects.requireNonNull(kieServices, 'could not load factory KieServices')

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem()
        RULE_FILES.each {
            kieFileSystem.write(ResourceFactory.newClassPathResource(it))
        }
        if (worldRulesDrl) {
            kieFileSystem.write('src/main/resources/org/patdouble/adventuregame/state/world.drl', worldRulesDrl)
        }
        if (worldRulesDslr) {
            kieFileSystem.write('src/main/resources/org/patdouble/adventuregame/state/world.dslr', worldRulesDslr)
        }
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem)
        kieBuilder.buildAll()
        KieModule kieModule = kieBuilder.getKieModule()

        KieContainer kContainer = kieServices.newKieContainer(kieModule.getReleaseId())

        KieBaseConfiguration kieBaseConfiguration = kieServices.newKieBaseConfiguration()
        kieBaseConfiguration.setOption(EqualityBehaviorOption.IDENTITY)

        kContainer.newKieBase(kieBaseConfiguration)
        kContainer
    }
}

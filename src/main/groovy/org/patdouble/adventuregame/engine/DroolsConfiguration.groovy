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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configures the rule engine.
 */
@Configuration
@CompileDynamic
class DroolsConfiguration {
    private static final List<String> RULE_FILES = [
            'org/patdouble/adventuregame/state/default.dsl',
            'org/patdouble/adventuregame/state/default.dslr',
            'org/patdouble/adventuregame/state/debug.drl',
            'org/patdouble/adventuregame/state/default.drl',
    ]

    @Bean
    KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get()

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem()
        RULE_FILES.each {
            kieFileSystem.write(ResourceFactory.newClassPathResource("rules/${it}"))
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

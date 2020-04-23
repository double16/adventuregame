package org.patdouble.adventuregame.ui.console

import ch.qos.logback.classic.Level
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import spock.lang.Specification

class MainTest extends Specification {
    Main main

    void setup() {
        main = new Main()
    }

    def "default config"() {
        when:
        main.configureViaCommandLine(new String[0])
        then:
        main.logLevel == Level.ERROR
    }

    def "log level info"() {
        when:
        main.configureViaCommandLine(['--info'] as String[])
        then:
        main.logLevel == Level.INFO
    }

    def "log level debug"() {
        when:
        main.configureViaCommandLine(['--debug'] as String[])
        then:
        main.logLevel == Level.DEBUG
    }

    def "chooseWorld picks Trailer Park"() {
        when:
        main.applyConfiguration()
        main.chooseWorld()
        then:
        main.world.name == YamlUniverseRegistry.TRAILER_PARK
    }
}

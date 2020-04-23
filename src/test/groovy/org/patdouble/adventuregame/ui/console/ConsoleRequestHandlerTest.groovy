package org.patdouble.adventuregame.ui.console

import ch.qos.logback.classic.Level
import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry

import java.util.concurrent.Flow

class ConsoleRequestHandlerTest extends AbstractConsoleTest {
    Main main
    Closure exitStrategy = { }

    void setup() {
        main = new Main()
        main.console = console
        main.logLevel = Level.INFO
        main.applyConfiguration()
        main.world = main.registry.worlds.find { it.name == YamlUniverseRegistry.TRAILER_PARK }
    }

    def cleanup() {
        main.engine.close()
    }

    def "onError"() {
        given:
        main.start(exitStrategy)
        Flow.Subscription subscription = Mock()
        ConsoleRequestHandler handler = new ConsoleRequestHandler(console, main.engine, exitStrategy)
        handler.onSubscribe(subscription)
        when:
        handler.onError(new IllegalArgumentException())
        then:
        errorData.toString().contains('IllegalArgumentException')
        subscription.cancel()
    }

    def "human player shadowblow"() {
        given:
        main.start(exitStrategy)
        when: 'Shadowblow'
        answer('Do you want to be this player', 'y\n')
        answer('Full Name?', '\n')
        answer('Nick Name?', '\n')
        and: 'Victor'
        answer('Do you want to be this player', 'n\n')
        and: 'thug'
        answer('Do you want to be this player', 'n\n')
        then:
        hasOutput('Entrance')
    }

    def "human player victor"() {
        given:
        main.start(exitStrategy)
        when: 'Shadowblow'
        answer('Do you want to be this player', 'n\n')
        and: 'Victor'
        answer('Do you want to be this player', 'y\n')
        answer('Full Name?', '\n')
        answer('Nick Name?', '\n')
        and: 'thug'
        answer('Do you want to be this player', 'n\n')
        then:
        hasOutput('Entrance')
    }

    def "meet goal"() {
        given:
        main.start(exitStrategy)
        when: 'Shadowblow'
        answer('Do you want to be this player', 'y\n')
        answer('Full Name?', '\n')
        answer('Nick Name?', '\n')
        and: 'Victor'
        answer('Do you want to be this player', 'n\n')
        and: 'thug'
        answer('Do you want to be this player', 'n\n')
        and: 'navigate to end room'
        answer('Shadowblow the warrior ?', 'go n\n')
        answer('Shadowblow the warrior ?', 'go e\n')
        answer('Shadowblow the warrior ?', 'go e\n')
        then:
        hasOutput('GAME OVER')
    }

    def "player notification"() {
        given:
        main.start(exitStrategy)
        when: 'Shadowblow'
        answer('Do you want to be this player', 'y\n')
        answer('Full Name?', '\n')
        answer('Nick Name?', '\n')
        and: 'Victor'
        answer('Do you want to be this player', 'n\n')
        and: 'thug'
        answer('Do you want to be this player', 'n\n')
        and: 'navigate to end room'
        answer('Shadowblow the warrior ?', 'go up\n')
        then:
        hasOutput('Things you can do')
    }
}

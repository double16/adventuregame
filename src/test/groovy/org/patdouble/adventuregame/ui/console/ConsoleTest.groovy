package org.patdouble.adventuregame.ui.console

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

class ConsoleTest extends AbstractConsoleTest {
    def "default terminal"() {
        when:
        Console defaultConsole = new Console()
        then:
        defaultConsole.output.is(AnsiConsole.out())
        defaultConsole.error.is(System.err)
        defaultConsole.terminal.getType() == 'dumb'
    }

    def "closing"() {
        when:
        console.close()
        then:
        _ * output.close()
        _ * error.close()
    }

    def "Flush"() {
        when:
        console.flush()
        then:
        _ * output.flush()
        _ * error.flush()
    }

    def "Print"() {
        when:
        console.print('hello')
        output.flush()
        then:
        outputData.toString() == 'hello'
    }

    def "Println"() {
        when:
        console.println('hello').println('world')
        output.flush()
        then:
        outputData.toString() == 'hello\nworld\n'
    }

    def "Print Ansi"() {
        when:
        console.print {
            newline()
                    .a(Ansi.Attribute.NEGATIVE_ON).a('--- GAME OVER ---').a(Ansi.Attribute.NEGATIVE_OFF)
                    .newline()
                    .reset() }
        then:
        outputData.toString() == '\n' +
                '\u001B[7m--- GAME OVER ---\u001B[27m\n' +
                '\u001B[m'
    }

    def "Println Ansi"() {
        when:
        console.println { a(Ansi.Attribute.NEGATIVE_ON).a('--- GAME OVER ---').a(Ansi.Attribute.NEGATIVE_OFF) }
        then:
        outputData.toString() == '\u001B[7m--- GAME OVER ---\u001B[27m\n'
    }

    def "Format"() {
        when:
        console.format('Hello %s', 'world')
        output.flush()
        then:
        outputData.toString() == 'Hello world'
    }

    def "ReadLine"() {
        given:
        inputData.withWriter { it.write('hello world\n') }
        expect:
        console.readLine() == 'hello world'
    }

    def "ReadLine with prompt"() {
        given:
        inputData.withWriter { it.write('hello world\n') }
        when:
        String s = console.readLine('Hello? ')
        then:
        s == 'hello world'
        outputData.toString() == 'hello world\r\n' +
                'Hello? hello world\r\r\n' +
                '\u001B[?2004l'
    }

    def "ReadLine with prompt and buffer"() {
        given:
        inputData.withWriter { it.write('hello world\n') }
        when:
        String s = console.readLine('Hello? ', 'hey')
        then:
        s == 'heyhello world'
        outputData.toString() == 'hello world\r\n' +
                'Hello? heyhello world\r\r\n' +
                '\u001B[?2004l'
    }
}

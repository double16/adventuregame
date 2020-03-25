package org.patdouble.adventuregame.ui.console

import org.jline.terminal.TerminalBuilder
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.Charset
import java.time.Duration

class AbstractConsoleTest extends Specification {
    Charset cs = Charset.forName('US-ASCII')
    ByteArrayOutputStream outputData
    PrintStream output
    ByteArrayOutputStream errorData
    PrintStream error
    PipedOutputStream inputData
    InputStream input
    Console console

    void setupSpec() {
        System.setProperty(TerminalBuilder.PROP_DUMB_COLOR, 'false')
    }

    void setup() {
        outputData = new ByteArrayOutputStream()
        output = new PrintStream(outputData, true, 'US-ASCII')
        errorData = new ByteArrayOutputStream()
        error = new PrintStream(errorData, true, 'US-ASCII')
        inputData = new PipedOutputStream()
        input = new PipedInputStream(inputData)
        console = new Console(output, error, input)
    }

    void cleanup() {
        console.close()
    }

    void hasOutput(String substring, int tries = 4, Duration interval = Duration.ofSeconds(3)) {
        for(int i = 0; i < tries; i++) {
            if (outputData.toString().contains(substring)) {
                outputData.reset()
                return
            }
            Thread.sleep(interval.toMillis())
        }
        assert outputData.toString().contains(substring)
    }

    void answer(String prompt, String answer) {
        hasOutput(prompt)
        inputData.write(cs.encode(answer).array())
    }
}

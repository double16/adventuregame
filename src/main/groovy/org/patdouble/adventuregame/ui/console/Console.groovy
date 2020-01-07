package org.patdouble.adventuregame.ui.console

import groovy.transform.CompileDynamic
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

/**
 * Replacement for @{link java.io.Console} but less restrictive, i.e. can be used with redirected I/O.
 */
@CompileDynamic
class Console implements AutoCloseable, Flushable {
    private final PrintStream output
    private final PrintStream error
    private final Terminal terminal
    private final LineReader reader

    Console(
            PrintStream output = null,
            PrintStream error = null,
            InputStream input = null) {

        if ((output == null || output.is(AnsiConsole.out())) && (input == null || input.is(System.in))) {
            this.output = output ?: AnsiConsole.out()
            terminal = TerminalBuilder.terminal()
        } else {
            this.output = output ?: AnsiConsole.out()
            terminal = TerminalBuilder.builder().system(false).streams(input ?: System.in, output).build()
        }
        this.error = error ?: System.err

        reader = LineReaderBuilder.builder().terminal(terminal).build()
    }

    @Override
    void close() throws Exception {
        this.output.close()
        this.error.close()
        this.terminal.close()
    }

    @Override
    void flush() throws IOException {
        this.output.flush()
        this.error.flush()
    }

    Console print(CharSequence s) {
        output.print(s as String)
        this
    }

    Console println(CharSequence s) {
        output.println(s as String)
        this
    }

    Console print(@DelegatesTo(Ansi) Closure<Ansi> ansiClosure) {
        ansiClosure.delegate = Ansi.ansi(terminal.width)
        output.print(ansiClosure.call() as String)
        this
    }

    Console println(@DelegatesTo(Ansi) Closure<Ansi> ansiClosure) {
        print(ansiClosure)
        output.println()
        this
    }

    Console format(String fmt, Object... args) {
        output.format(fmt, args).flush()
        this
    }

    String readLine() {
        reader.readLine()
    }

    String readLine(String prompt, String buffer = null) {
        reader.readLine(prompt, null, buffer)
    }
}

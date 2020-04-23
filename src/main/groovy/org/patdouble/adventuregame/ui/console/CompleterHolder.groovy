package org.patdouble.adventuregame.ui.console

import groovy.transform.CompileStatic
import org.jline.reader.Completer

import javax.validation.constraints.NotNull

/**
 * Holds another Completer allowing it to be changed. This is needed because LineReader
 * does not allow the completer to be changed.
 */
@CompileStatic
class CompleterHolder implements Completer {
    @Delegate
    @NotNull
    Completer underlying

    CompleterHolder(Completer underlying) {
        Objects.requireNonNull(underlying)
        this.underlying = underlying
    }
}

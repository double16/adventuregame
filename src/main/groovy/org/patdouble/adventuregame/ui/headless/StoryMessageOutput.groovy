package org.patdouble.adventuregame.ui.headless

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.flow.AbstractSubscriber
import org.patdouble.adventuregame.flow.StoryMessage

/**
 * Prints out StoryMessage content to the console.
 */
@CompileStatic
class StoryMessageOutput extends AbstractSubscriber {
    final PrintStream printer

    StoryMessageOutput(PrintStream printer = System.out) {
        this.printer = printer
    }

    @Override
    void onNext(StoryMessage item) {
        printer.println item.toString()
        super.onNext(item)
    }

    @Override
    void onError(Throwable throwable) {
        throwable.printStackTrace(printer)
        super.onError(throwable)
    }
}

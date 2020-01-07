package org.patdouble.adventuregame.ui.headless

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.flow.StoryMessage

import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscription

/**
 * Prints out StoryMessage content to the console.
 */
@CompileDynamic
class StoryMessageOutput implements Flow.Subscriber<StoryMessage>, AutoCloseable {
    final PrintStream printer
    private Subscription subscription

    StoryMessageOutput(PrintStream printer = System.out) {
        this.printer = printer
    }

    @Override
    void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription
        subscription.request(1)
    }

    @Override
    void onNext(StoryMessage item) {
        printer.println item.toString()
        subscription.request(1)
    }

    @Override
    void onError(Throwable throwable) {
        printer.println throwable.toString()
        subscription.request(1)
    }

    @Override
    void onComplete() {
        // nothing to do
    }

    @Override
    void close() throws Exception {
        subscription.cancel()
    }
}

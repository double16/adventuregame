package org.patdouble.adventuregame

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.flow.AbstractSubscriber
import org.patdouble.adventuregame.flow.StoryMessage

import java.util.concurrent.Flow

/**
 * Counts the number and last time of messages. Intended to be used with
 * {@link SpecHelper#settle(int, java.time.Duration, groovy.lang.Closure)}.
 */
@CompileStatic
class FlowSubscriptionCounter extends AbstractSubscriber {
    int count
    long lastMessageMillis

    @Override
    void onNext(StoryMessage item) {
        count++
        lastMessageMillis = System.currentTimeMillis()
        super.onNext(item)
    }

    @Override
    void onError(Throwable throwable) {
        lastMessageMillis = System.currentTimeMillis()
    }

    Closure dataHash() {
        { -> lastMessageMillis }
    }
}

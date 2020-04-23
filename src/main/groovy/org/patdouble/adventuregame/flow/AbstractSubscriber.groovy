package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic

import java.util.concurrent.Flow

/**
 * Base class for simplifying implementation of subscribers.
 */
@CompileStatic
class AbstractSubscriber implements Flow.Subscriber<StoryMessage>, AutoCloseable {
    private Flow.Subscription subscription

    protected AbstractSubscriber() { }

    @Override
    void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription
        subscription.request(1)
    }

    @Override
    void onNext(StoryMessage item) {
        subscription.request(1)
    }

    @Override
    void onError(Throwable throwable) {
        subscription.request(1)
    }

    @Override
    void onComplete() { }

    @Override
    void close() throws Exception {
        subscription.cancel()
    }
}

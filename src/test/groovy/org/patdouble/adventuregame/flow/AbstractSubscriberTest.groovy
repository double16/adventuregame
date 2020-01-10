package org.patdouble.adventuregame.flow

import spock.lang.Specification

import java.util.concurrent.Flow

class AbstractSubscriberTest extends Specification {
    AbstractSubscriber subscriber
    Flow.Subscription subscription

    void setup() {
        subscriber = new AbstractSubscriber() { }
        subscription = Mock()
        subscriber.onSubscribe(subscription)
    }

    def "OnSubscribe"() {
        when:
        subscriber.onSubscribe(subscription)
        then:
        1 * subscription.request(1)
    }

    def "OnNext"() {
        when:
        subscriber.onNext(new Notification('sub', 'text'))
        then:
        1 * subscription.request(1)
    }

    def "OnError"() {
        when:
        subscriber.onError(new IllegalArgumentException())
        then:
        1 * subscription.request(1)
    }

    def "OnComplete"() {
        when:
        subscriber.onComplete()
        then:
        notThrown(Exception)
    }

    def "Close"() {
        when:
        subscriber.close()
        then:
        1 * subscription.cancel()
    }
}

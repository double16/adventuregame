package org.patdouble.adventuregame.flow

import org.patdouble.adventuregame.engine.Engine
import spock.lang.Specification

import java.util.concurrent.Flow

class EngineCloseOnStoryEndTest extends Specification {
    Engine engine
    EngineCloseOnStoryEnd subscriber
    Flow.Subscription subscription

    void setup() {
        engine = Mock()
        subscription = Mock()
        subscriber = new EngineCloseOnStoryEnd(engine)
        subscriber.onSubscribe(subscription)
    }

    void "onNext notification"() {
        when:
        subscriber.onNext(new Notification('sub', 'text'))
        then:
        0 * engine.close()
        and:
        1 * subscription.request(1)
    }

    void "onNext StoryEnded"() {
        when:
        subscriber.onNext(new StoryEnded())
        then:
        1 * engine.close()
        and:
        1 * subscription.request(1)
    }
}

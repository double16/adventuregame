package org.patdouble.adventuregame.ui.headless

import org.patdouble.adventuregame.flow.Notification
import spock.lang.Specification

import java.util.concurrent.Flow

class StoryMessageOutputTest extends Specification {
    ByteArrayOutputStream byteout
    PrintStream printer
    StoryMessageOutput output
    Flow.Subscription subscription

    def setup() {
        byteout = new ByteArrayOutputStream()
        printer = new PrintStream(byteout)
        output = new StoryMessageOutput(printer)
        subscription = Mock()
        output.onSubscribe(this.subscription)
    }

    def "OnNext"() {
        when:
        output.onNext(new Notification('subject', 'text'))
        then:
        byteout.toString() == 'Notification(subject:subject, text:text, type:Notification)\n'
        and:
        1 * subscription.request(1)
    }

    def "OnError"() {
        when:
        output.onError(new IllegalArgumentException('test exception'))
        String s = new String(byteout.toString())
        then:
        s.contains('IllegalArgumentException')
        s.contains('test exception')
        and:
        0 * subscription.request(1)
    }
}

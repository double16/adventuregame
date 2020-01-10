package org.patdouble.adventuregame.ui.headless

import org.patdouble.adventuregame.flow.Notification
import spock.lang.Specification

import java.util.concurrent.Flow

class StoryMessageOutputTest extends Specification {
    ByteArrayOutputStream byteout
    PrintStream printer
    StoryMessageOutput output

    def setup() {
        byteout = new ByteArrayOutputStream()
        printer = new PrintStream(byteout)
        output = new StoryMessageOutput(printer)
        Flow.Subscription subscription = Mock()
        output.onSubscribe(subscription)
    }

    def "OnNext"() {
        when:
        output.onNext(new Notification('subject', 'text'))
        then:
        byteout.toString() == 'Notification(subject:subject, text:text)\n'
    }

    def "OnError"() {
        when:
        output.onError(new IllegalArgumentException('test exception'))
        String s = new String(byteout.toString())
        then:
        s.contains('IllegalArgumentException')
        s.contains('test exception')
    }
}

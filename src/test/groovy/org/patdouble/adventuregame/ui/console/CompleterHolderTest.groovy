package org.patdouble.adventuregame.ui.console

import org.jline.reader.Completer
import spock.lang.Specification

class CompleterHolderTest extends Specification {

    def "require non-null"() {
        when:
        new CompleterHolder(null)
        then:
        thrown(NullPointerException)
    }

    def "delegate methods"() {
        given:
        Completer completer = Mock()
        CompleterHolder holder = new CompleterHolder(completer)
        when:
        holder.complete(null, null, null)
        then:
        1 * completer.complete(null, null, null)
    }
}

package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.ToString
import org.patdouble.adventuregame.state.Player

@Canonical
@ToString(includePackage = false, includeNames = true)
class PlayerChanged extends StoryMessage {
    final Player player
    final long chronos
}

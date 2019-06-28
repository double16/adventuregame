package org.patdouble.adventuregame.model

import groovy.transform.Canonical
import org.patdouble.adventuregame.state.Player

@Canonical
class Challenge {
    String description

    List<Choice> choices = []

    void choose(Player player, Choice choice) {
        choice.apply(player)
    }
}

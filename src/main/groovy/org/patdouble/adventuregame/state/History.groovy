package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.model.World

class History {
    World world
    List<Player> cast
    List<String> events = new ArrayList<>()

    History(World world, List<Player> cast) {
        this.world = world
        this.cast = cast
    }

    void addStoryLine(String s) {
        events.add(s)
    }
}

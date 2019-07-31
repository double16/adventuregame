package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.model.World

class History {
    World world
    List<Event> events = []

    History(World world) {
        this.world = world
    }

    void addEvent(Event e) {
        events << e
    }
}

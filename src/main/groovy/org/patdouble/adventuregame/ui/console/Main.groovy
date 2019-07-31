package org.patdouble.adventuregame.ui.console

import org.patdouble.adventuregame.Engine
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.storage.yaml.WorldYamlStorage

class Main {
    /** Used to stop the main thread because this is a reactive application. */
    private static final Object WAIT_LOCK = new Object()

    static void main(String[] args) {
        World world = new WorldYamlStorage().load(WorldYamlStorage.getResourceAsStream('/worlds/trailer-park.yml'))
        Story story = new Story(world)
        Engine engine = new Engine(story)
        engine.autoLifecycle = true
        engine.subscribe(new ConsoleRequestHandler(new Console(), engine))
//        engine.subscribe(new StoryMessageOutput())
        engine.init()

        synchronized (WAIT_LOCK) {
            WAIT_LOCK.wait()
        }
    }
}

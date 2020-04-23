package org.patdouble.adventuregame.flow

import groovy.transform.CompileStatic
import org.patdouble.adventuregame.engine.Engine

/**
 * Close the Engine when the story ends.
 */
@CompileStatic
class EngineCloseOnStoryEnd extends AbstractSubscriber {
    private final Engine engine

    EngineCloseOnStoryEnd(Engine engine) {
        Objects.requireNonNull(engine)
        this.engine = engine
    }

    @Override
    @SuppressWarnings('Instanceof')
    void onNext(StoryMessage item) {
        if (item instanceof StoryEnded) {
            engine.close()
        }
        super.onNext(item)
    }
}

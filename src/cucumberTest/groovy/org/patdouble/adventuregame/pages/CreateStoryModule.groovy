package org.patdouble.adventuregame.pages

import geb.Module

/**
 * Module on "Create New Story" page that contains info and controls for creating a story for a single world.
 */
class CreateStoryModule extends Module {
    static content = {
        title { $('.card-title').text() }
        description { $('.card-text').text() }
        startButton { $('button.btn-primary') }
    }
}

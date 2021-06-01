package org.patdouble.adventuregame.pages

import geb.Module

/**
 * Module for choosing a player.
 */
class PlayerChoiceModule extends Module {
    static content = {
        title { $('.card-title').text() }
        select { $('button.btn-primary') }
    }
}

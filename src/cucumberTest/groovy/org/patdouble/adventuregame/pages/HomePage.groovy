package org.patdouble.adventuregame.pages

import geb.Page

class HomePage extends Page {
    static url = '#/'
    static at = { $('div.story-create h1').text() == 'Create a New Story' }
    static content = {
        stories { $('div.story-create-item').moduleList(CreateStoryModule) }
    }
}

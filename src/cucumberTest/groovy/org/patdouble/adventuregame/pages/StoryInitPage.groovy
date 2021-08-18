package org.patdouble.adventuregame.pages

import geb.Page

class StoryInitPage extends Page {
    static at = { $('div.story-init') }
    static content = {
        players { $('div.player-choice').moduleList(PlayerChoiceModule) }
        fullName { $('input.full-name') }
        nickName { $('input.nick-name') }
        playerSubmit { $('form.player-choice button.btn-primary') }
        detail { $('div.player-detail').module(PlayerDetailModule) }
        startStory { $('button.start-story') }
        linkInvite { $('span.link-invite') }
        linkContinue { $('span.link-continue') }
    }
}

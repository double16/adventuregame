package org.patdouble.adventuregame.pages

import geb.Page

class StoryRunPage extends Page {
    static at = { $('div.story-run') }
    static content = {
        linkContinue { $('span.link-continue') }
        notifications { $('div.notification').moduleList(NotificationModule) }
        actionRequest { $('div.action-request').module(ActionRequestModule) }
    }
}

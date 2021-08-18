package org.patdouble.adventuregame.pages

import geb.Module

class NotificationModule extends Module {
    static content = {
        message { $('span.message').text() }
        close { $('button.close') }
    }
}

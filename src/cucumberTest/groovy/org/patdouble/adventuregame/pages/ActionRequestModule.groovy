package org.patdouble.adventuregame.pages

import geb.Module

class ActionRequestModule extends Module {
    static content = {
        roomName { $('.room-name').text() }
        roomDescription { $('room-description').text() }
        roomOccupants { $('room-occupants') }
        statement { $('input.statement') }
        submit { $('form.action-request button.btn-primary') }
        actionHelp { $('.action-help') }
        actionHelpClose { $('.action-help-close') }
        actions { $('button.action') }
    }
}

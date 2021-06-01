package org.patdouble.adventuregame.pages

import geb.Module

class PlayerDetailModule extends Module {
    static content = {
        fullName { $('.full-name').text() }
        nickName { $('.nick-name').text() }
        wealth { $('.wealth') }
        health { $('.health') }
        virtue { $('.virtue') }
        memory { $('.memory') }
        bravery { $('.bravery') }
        leadership { $('.leadership') }
        experience { $('.experience') }
        agility { $('.agility') }
        speed { $('.speed') }
    }
}

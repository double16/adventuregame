package org.patdouble.adventuregame.model

class GoalMocks {
    static final Goal STORY_ENTER_ROOM = new Goal(
            name: 'enter_trailer_4',
            description: 'Visit room Trailer 4',
            rules: ['player enters room "trailer_4"']
    )

    static final Goal PLAYER_ENTER_ROOM = new Goal(
            name: 'goto_dump',
            description: 'Find the Dump',
            rules: ['player goes to room "dump"']
    )
}

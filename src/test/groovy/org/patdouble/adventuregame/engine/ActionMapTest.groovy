package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.MapMessage
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.model.World
import org.patdouble.adventuregame.state.Motivator
import spock.lang.PendingFeature

class ActionMapTest extends AbstractPlayerTest {
    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    @Override
    void modify(World world) {
        world.goals.each { it.required = false }
    }

    def setup() {
        engine.chronosLimit = 5
    }

    def "thief has initial map"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'map').join()
        boolean success = engine.action(thief, 'map').join()
        waitForMessages()

        then:
        success
        1 * storySubscriber.onNext(new MapMessage(player: thief, rooms: [
                new MapMessage.RoomInfo('entrance', 'Entrance', null),
                new MapMessage.RoomInfo('trailer_1', 'Trailer 1', null),
                new MapMessage.RoomInfo('trailer_2', 'Trailer 2', null),
                new MapMessage.RoomInfo('trailer_3', 'Trailer 3', null),
                new MapMessage.RoomInfo('trailer_4', 'Trailer 4', null),
                new MapMessage.RoomInfo('dump', 'Dump', null),
        ],
                edges: [
                        new MapMessage.RoomEdgeInfo(from:'entrance', to:'trailer_2', direction:'north', back:'south'),
                        new MapMessage.RoomEdgeInfo(from:'trailer_1', to:'trailer_2', direction:'east', back:'west'),
                        new MapMessage.RoomEdgeInfo(from:'dump', to:'trailer_3', direction:'dive'),
                        new MapMessage.RoomEdgeInfo(from:'trailer_3', to:'trailer_2', direction:'west', back:'east'),
                        new MapMessage.RoomEdgeInfo(from:'trailer_3', to:'trailer_4', direction:'east', back:'west'),
                        new MapMessage.RoomEdgeInfo(from:'trailer_4', to:'dump', direction:'north', back:'down'),
                ],
                regions: [] as Set
        ))
        and: 'chronos did not move'
        engine.story.chronos.current == 1
        0 * storySubscriber.onNext(new PlayerChanged(thief, 1))
        1 * storySubscriber.onNext { it instanceof RequestCreated && it.request.player == thief }
    }

    @PendingFeature
    def "known regions included"() {

    }
}

package org.patdouble.adventuregame.engine

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.patdouble.adventuregame.flow.MapMessage
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.state.Motivator
import org.slf4j.LoggerFactory
import spock.lang.Unroll

import java.time.Duration
import java.time.temporal.ChronoUnit

@Unroll
class RoomMemoryTest extends AbstractPlayerTest {
    Level savedLogLevel

    @Override
    Motivator getDefaultMotivator() {
        Motivator.HUMAN
    }

    def setup() {
        savedLogLevel = ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).level
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN)
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(savedLogLevel)
    }

    def "initial placement"() {
        when:
        engine.start().join()

        then:
        engine.findRoomsKnownToPlayer(warrior).join()*.modelId.sort() == ['entrance']
    }

    def "reach goal"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'go north').join()
        engine.action(thief, 'wait').join()
        engine.action(warrior, 'go east').join()
        engine.action(thief, 'wait').join()
        engine.action(warrior, 'go east').join()

        then:
        engine.findRoomsKnownToPlayer(warrior).join()*.modelId.sort() == ['entrance', 'trailer_2', 'trailer_3', 'trailer_4']
    }

    def "revisit rooms with map command"() {
        given:
        engine.start().join()

        when:
        engine.action(warrior, 'go north').join()
        engine.action(thief, 'wait').join()
        engine.action(warrior, 'go south').join()
        engine.action(thief, 'wait').join()

        then:
        engine.findRoomsKnownToPlayer(warrior).join()*.modelId.sort() == ['entrance', 'trailer_2']
        engine.story.chronos.current == 3

        when:
        boolean mapSuccess = engine.action(warrior, 'map').join()
        engine.action(thief, 'map').join()
        waitForMessages()

        then:
        mapSuccess
        1 * storySubscriber.onNext(new MapMessage(player: warrior, rooms: [
                new MapMessage.RoomInfo('entrance', 'Entrance', null),
                new MapMessage.RoomInfo('trailer_2', 'Trailer 2', null),
            ],
            edges: [
                    new MapMessage.RoomEdgeInfo('entrance', 'trailer_2', 'north', 'south'),
                    new MapMessage.RoomEdgeInfo('trailer_2', 'entrance', 'south', 'north'),
            ]
        ))
        and: 'chronos did not move'
        engine.story.chronos.current == 3
        0 * storySubscriber.onNext(new PlayerChanged(warrior, 4))
        1 * storySubscriber.onNext { it instanceof RequestCreated && it.request.player == warrior }
    }

    def "forget (#forget) visited room at memory level #memory, chronos #lastChronos, visited count #count"() {
        given:
        engine.chronosLimit = (count+lastChronos)*3+100
        warrior.memory = memory
        engine.start().join()

        when:
        engine.action(warrior, 'go north').join()
        engine.action(thief, 'wait').join()

        if (count > 0) {
            for(int i : 1..count) {
                engine.action(warrior, 'go east').join()
                engine.action(thief, 'wait').join()
                engine.action(warrior, 'go west').join()
                engine.action(thief, 'wait').join()
            }
        }
        Long trailer3Chronos = engine.story.history.events
                .collect { it.players }.flatten()
                .findAll { it.player.room.modelId == 'trailer_3' }
                .collect { it.event.when }
                .max()

        long adjustedLastChronos = lastChronos + (trailer3Chronos ?: 0)

        while (engine.story.chronos.current < adjustedLastChronos) {
            engine.action(warrior, 'go west').join()
            engine.action(thief, 'wait').join()
            if (engine.story.chronos.current < adjustedLastChronos) {
                engine.action(warrior, 'go east').join()
                engine.action(thief, 'wait').join()
            }
        }

        engine.waitForFiringFinish(Duration.of(10, ChronoUnit.SECONDS))

        then:
        engine.findRoomsKnownToPlayer(warrior).join()*.modelId.contains('trailer_3') != forget

        where:
        memory | count | lastChronos | forget
        500    | 0     | 1           | true
        500    | 1     | 83          | false
        500    | 1     | 84          | true
        500    | 2     | 133         | false
        500    | 2     | 134         | true
        500    | 100   | 416         | false
        500    | 100   | 417         | true

        250    | 0     | 1           | true
        250    | 1     | 20          | false
        250    | 1     | 21          | true
        250    | 2     | 33          | false
        250    | 2     | 34          | true
        250    | 100   | 104         | false
        250    | 100   | 105         | true

        750    | 0     | 1           | true
        750    | 1     | 187         | false
        750    | 1     | 188         | true
        750    | 2     | 300         | false
        750    | 2     | 301         | true

        0      | 0     | 2           | true
        0      | 1     | 2           | true
        0      | 100   | 2           | true

        1000   | 0     | 1           | true
        1000   | 1     | 333         | false
        1000   | 1     | 334         | true
        1000   | 2     | 534         | false
        1000   | 2     | 535         | true
        1000   | 100   | 1666        | false
        1000   | 100   | 1667        | true
    }
}

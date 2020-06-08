package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.model.RoomMocks
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class EventTest extends Specification {
    def "ComputeSecureHash with #action"() {
        given:
        Event e1 = new Event()
        e1.when = 1
        e1.players << new PlayerEvent(event: e1,
                player: new Player(persona: PersonaMocks.WARRIOR, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )
        e1.players << new PlayerEvent(event: e1,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )

        Event e2 = new Event()
        e2.when = 2
        e2.players << new PlayerEvent(event: e2,
                player: new Player(persona: PersonaMocks.WARRIOR, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )
        e2.players << new PlayerEvent(event: e2,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )

        Event e3 = new Event()
        e3.when = 3
        e3.players << new PlayerEvent(event: e3,
                player: new Player(persona: PersonaMocks.WARRIOR, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: new ActionStatement(verb: 'go', directObject: 'north')
        )
        e3.players << new PlayerEvent(event: e3,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )

        expect:
        e1.computeSecureHash() == e2.computeSecureHash()
        e1.computeSecureHash() != e3.computeSecureHash()
        e2.computeSecureHash() != e3.computeSecureHash()

        where:
        action | _
        new ActionStatement(verb: 'wait') | _
        new ActionStatement(verb: 'go', directObject:  'south') | _
        new ActionStatement(verb: 'fight', directObject:  'thug', indirectObject: 'club') | _
    }

    def "ComputeSecureHash persona changes"() {
        given:
        ActionStatement action = new ActionStatement(verb: 'wait')
        Event e1 = new Event()
        e1.when = 1
        e1.players << new PlayerEvent(event: e1,
                player: new Player(persona: PersonaMocks.WARRIOR, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )
        e1.players << new PlayerEvent(event: e1,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )

        Event e2 = new Event()
        e2.when = 2
        e2.players << new PlayerEvent(event: e2,
                player: new Player(persona: PersonaMocks.WARRIOR.clone().setHealth(10), motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )
        e2.players << new PlayerEvent(event: e2,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )

        expect:
        e1.computeSecureHash() != e2.computeSecureHash()
    }

    def "ComputeSecureHash room changes"() {
        given:
        ActionStatement action = new ActionStatement(verb: 'wait')
        Event e1 = new Event()
        e1.when = 1
        e1.players << new PlayerEvent(event: e1,
                player: new Player(persona: PersonaMocks.WARRIOR, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )
        e1.players << new PlayerEvent(event: e1,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )

        Event e2 = new Event()
        e2.when = 2
        e2.players << new PlayerEvent(event: e2,
                player: new Player(persona: PersonaMocks.WARRIOR, motivator: Motivator.AI, room: RoomMocks.ENTRANCE),
                action: action
        )
        e2.players << new PlayerEvent(event: e2,
                player: new Player(persona: PersonaMocks.THIEF, motivator: Motivator.AI, room: RoomMocks.TRAILER1),
                action: action
        )

        expect:
        e1.computeSecureHash() != e2.computeSecureHash()
    }
}

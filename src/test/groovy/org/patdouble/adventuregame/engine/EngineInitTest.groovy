package org.patdouble.adventuregame.engine

import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest

class EngineInitTest extends EngineTest {
    def "Init"() {
        when:
        engine.init()

        then: 'player requests'
        story.requests.size() == 12
        with(story.requests[0] as PlayerRequest) {
            template.persona.name == 'warrior'
            (!optional)
        }
        with(story.requests[1] as PlayerRequest) {
            template.persona.name == 'thief'
            (!optional)
        }
        story.requests[2..11].collect { it.template.persona.name } == ['thug']*10
        story.requests[2..11].collect { it.optional } == [true]*10

        and: 'initial chronos'
        story.chronos.current == 0

        and: 'empty history'
        story.history
        story.history.world == story.world
        story.history.events.empty

        and :'goal status'
        story.goals.size() == 3
        !story.goals.find { it.fulfilled }
        story.goals.find { it.goal.name == 'one' }
        story.goals.find { it.goal.name == 'two' }
        story.goals.find { it.goal.name == 'three' }

        and: 'notifications'
        12 * storySubscriber.onNext({ it instanceof RequestCreated })

        and: 'not closed'
        !engine.isClosed()
    }

    def "addToCast valid"() {
        given:
        engine.init()
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thugRequest = story.requests.find { it.template.persona.name == 'thug' } as PlayerRequest

        when:
        Player warrior = warriorRequest.template.createPlayer(Motivator.AI)
        engine.addToCast(warrior)

        then:
        story.cast.contains(warrior)

        and:
        !story.requests.find { it.template.persona.name == 'warrior' }
        story.requests.find { it.template.persona.name == 'thief' }
        story.requests.findAll { it.template.persona.name == 'thug' }.size() == 10

        and:
        1 * storySubscriber.onNext(new RequestSatisfied(warriorRequest))

        when:
        Player thug = thugRequest.template.createPlayer(Motivator.AI)
        engine.addToCast(thug)

        then:
        story.cast.contains(thug)
        and:
        !story.requests.find { it.template.persona.name == 'warrior' }
        story.requests.find { it.template.persona.name == 'thief' }
        story.requests.findAll { it.template.persona.name == 'thug' }.size() == 9
        and:
        1 * storySubscriber.onNext(new RequestSatisfied(thugRequest))
    }

    def "addToCast invalid"() {
        given:
        engine.init()

        when:
        PlayerRequest request = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        Player warrior = request.template.createPlayer(Motivator.AI)
        engine.addToCast(warrior)
        engine.addToCast(warrior.clone())

        then:
        thrown(IllegalArgumentException)
        and:
        0 * storySubscriber.onNext(new RequestSatisfied(request))
    }

    def "ignore optional"() {
        given:
        engine.init()
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thugRequest = story.requests.find { it.template.persona.name == 'thug' } as PlayerRequest

        when:
        Player warrior = warriorRequest.template.createPlayer(Motivator.AI)
        engine.addToCast(warrior)

        then:
        story.cast.contains(warrior)

        and:
        !story.requests.find { it.template.persona.name == 'warrior' }
        story.requests.find { it.template.persona.name == 'thief' }
        story.requests.findAll { it.template.persona.name == 'thug' }.size() == 10

        and:
        1 * storySubscriber.onNext(new RequestSatisfied(warriorRequest))

        when:
        engine.ignore(thugRequest)

        then:
        !story.cast.find { it.persona.name == 'thug' }

        and:
        !story.requests.find { it.template.persona.name == 'warrior' }
        story.requests.find { it.template.persona.name == 'thief' }
        story.requests.findAll { it.template.persona.name == 'thug' }.size() == 9
        and:
        1 * storySubscriber.onNext(new RequestSatisfied(thugRequest))
    }

    def "ignore required"() {
        given:
        engine.init()
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest

        when:
        engine.ignore(warriorRequest)

        then:
        thrown(IllegalArgumentException)

        and:
        story.requests.find { it.template.persona.name == 'warrior' }
        story.requests.find { it.template.persona.name == 'thief' }
        story.requests.findAll { it.template.persona.name == 'thug' }.size() == 10

        and:
        0 * storySubscriber.onNext(new RequestSatisfied(warriorRequest))
    }

    def "autostart"() {
        given:
        engine.autoLifecycle = true
        engine.init()
        engine.story.goals.each { it.goal.required = false }
        PlayerRequest warriorRequest = story.requests.find { it.template.persona.name == 'warrior' } as PlayerRequest
        PlayerRequest thiefRequest = story.requests.find { it.template.persona.name == 'thief' } as PlayerRequest

        when:
        engine.addToCast(warriorRequest.template.createPlayer(Motivator.AI))
        engine.addToCast(thiefRequest.template.createPlayer(Motivator.AI))
        story.requests.findAll { it.template.persona.name == 'thug' }. each {
            engine.addToCast(it.template.createPlayer(Motivator.AI))
        }

        then:
        story.requests.isEmpty()
        !engine.isClosed()
        !story.goals.isEmpty()

        when:
        engine.close()

        then:
        engine.isClosed()
    }

    def "resendRequests"() {
        when:
        engine.init()
        engine.resendRequests()
        then: 'notifications'
        24 * storySubscriber.onNext({ it instanceof RequestCreated })
    }

    def "init idempotency"() {
        given:
        engine.init()
        int requestCount = engine.story.requests.size()
        int goalStatusCount = engine.story.goals.size()
        int castCount = engine.story.cast.size()

        when:
        engine.init()
        then:
        requestCount == engine.story.requests.size()
        goalStatusCount == engine.story.goals.size()
        castCount == engine.story.cast.size()
    }

    def "start idempotency"() {
        given:
        engine.init()
        engine.story.requests
            .findAll { (it instanceof PlayerRequest) && !it.optional }
            .each { engine.addToCast(it.template.createPlayer(Motivator.HUMAN)) }
        engine.start()
        int requestCount = engine.story.requests.size()
        int goalStatusCount = engine.story.goals.size()
        int castCount = engine.story.cast.size()

        when:
        engine.start()
        then:
        requestCount == engine.story.requests.size()
        goalStatusCount == engine.story.goals.size()
        castCount == engine.story.cast.size()

    }

    def "end idempotency"() {
        given:
        engine.init()
        engine.story.requests
                .findAll { (it instanceof PlayerRequest) && !it.optional }
                .each { engine.addToCast(it.template.createPlayer(Motivator.HUMAN)) }
        engine.start()
        engine.end()
        int requestCount = engine.story.requests.size()
        int goalStatusCount = engine.story.goals.size()
        int castCount = engine.story.cast.size()

        when:
        engine.end()
        then:
        requestCount == engine.story.requests.size()
        goalStatusCount == engine.story.goals.size()
        castCount == engine.story.cast.size()

    }
}

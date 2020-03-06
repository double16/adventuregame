package org.patdouble.adventuregame.storage.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.patdouble.adventuregame.engine.EngineTest
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.Notification
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.flow.StoryEnded
import org.patdouble.adventuregame.model.PersonaMocks
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.springframework.test.util.JsonPathExpectationsHelper

class JsonSerializationTest extends EngineTest {
    ObjectMapper mapper

    @Override
    def setup() {
        engine.init()
        mapper = new ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    @Override
    def cleanup() {
        engine.close()
    }

    def "RequestSatisfied"() {
        given:
        PlayerRequest req = engine.story.requests.find { it instanceof PlayerRequest && it.template.fullName == PersonaMocks.SHADOWBLOW_FULLNAME }
        RequestSatisfied requestSatisfied = new RequestSatisfied(req)

        when:
        String json = mapper.writeValueAsString(requestSatisfied)
        then:
        new JsonPathExpectationsHelper('$.request.@class').assertValue(json, '.PlayerRequest')
        new JsonPathExpectationsHelper('$.request.template.persona').doesNotExist(json)

        when:
        RequestSatisfied read = mapper.readValue(json, RequestSatisfied)
        then:
        ((PlayerRequest) read.request).template.fullName == PersonaMocks.SHADOWBLOW_FULLNAME
        and:
        !read.is(requestSatisfied)
        read == requestSatisfied
    }

    def "RequestCreated"() {
        given:
        PlayerRequest req = engine.story.requests.find { it instanceof PlayerRequest && it.template.fullName == PersonaMocks.SHADOWBLOW_FULLNAME }
        RequestCreated requestCreated = new RequestCreated(req)

        when:
        String json = mapper.writeValueAsString(requestCreated)
        then:
        new JsonPathExpectationsHelper('$.request.@class').assertValue(json, '.PlayerRequest')
        new JsonPathExpectationsHelper('$.request.template.persona').doesNotExist(json)

        when:
        RequestCreated read = mapper.readValue(json, RequestCreated)
        then:
        ((PlayerRequest) read.request).template.fullName == PersonaMocks.SHADOWBLOW_FULLNAME
        and:
        !read.is(requestCreated)
        read == requestCreated
    }

    def "PlayerChanged"() {
        given:
        PlayerRequest req = engine.story.requests.find { it instanceof PlayerRequest && it.template.fullName == PersonaMocks.SHADOWBLOW_FULLNAME }
        Player player = req.template.createPlayer(Motivator.HUMAN)
        engine.addToCast(player)
        PlayerChanged playerChanged = new PlayerChanged(player, 2)

        when:
        String json = mapper.writeValueAsString(playerChanged)
        then:
        new JsonPathExpectationsHelper('$.chronos').assertValue(json, 2)
        new JsonPathExpectationsHelper('$.player.fullName').exists(json)

        when:
        PlayerChanged read = mapper.readValue(json, PlayerChanged)
        then:
        read.chronos == 2
        read.player.fullName == PersonaMocks.SHADOWBLOW_FULLNAME
        and:
        !read.is(playerChanged)
        read == playerChanged
    }

    def "ChronosChanged"() {
        given:
        ChronosChanged chronosChanged = new ChronosChanged(2)

        when:
        String json = mapper.writeValueAsString(chronosChanged)
        then:
        new JsonPathExpectationsHelper('$.current').assertValue(json, 2)

        when:
        ChronosChanged read = mapper.readValue(json, ChronosChanged)
        then:
        read.current == 2
        and:
        !read.is(chronosChanged)
        read == chronosChanged
    }

    def "PlayerNotification"() {
        given:
        PlayerRequest req = engine.story.requests.find { it instanceof PlayerRequest && it.template.fullName == PersonaMocks.SHADOWBLOW_FULLNAME }
        Player player = req.template.createPlayer(Motivator.HUMAN)
        PlayerNotification pn = new PlayerNotification(player: player, subject: 'sub', text: 'txt')

        when:
        String json = mapper.writeValueAsString(pn)
        then:
        new JsonPathExpectationsHelper('$.subject').assertValue(json, 'sub')
        new JsonPathExpectationsHelper('$.text').assertValue(json, 'txt')
        new JsonPathExpectationsHelper('$.player.fullName').exists(json)

        when:
        PlayerNotification read = mapper.readValue(json, PlayerNotification)
        then:
        read.subject == 'sub'
        read.text == 'txt'
        read.player.fullName == PersonaMocks.SHADOWBLOW_FULLNAME
        and:
        !read.is(pn)
        read == pn
    }

    def "Notification"() {
        given:
        Notification n = new Notification(subject: 'sub', text: 'txt')

        when:
        String json = mapper.writeValueAsString(n)
        then:
        new JsonPathExpectationsHelper('$.subject').assertValue(json, 'sub')
        new JsonPathExpectationsHelper('$.text').assertValue(json, 'txt')

        when:
        Notification read = mapper.readValue(json, Notification)
        then:
        read.subject == 'sub'
        read.text == 'txt'
        and:
        !read.is(n)
        read == n
    }

    def "StoryEnded"() {
        given:
        StoryEnded storyEnded = new StoryEnded()

        when:
        String json = mapper.writeValueAsString(storyEnded)
        then:
        json

        when:
        StoryEnded read = mapper.readValue(json, StoryEnded)
        then:
        !read.is(storyEnded)
        read == storyEnded
    }
}

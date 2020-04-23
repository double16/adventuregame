package org.patdouble.adventuregame.storage.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.Canonical
import spock.lang.Specification

class IntRangeJsonSerializerTest extends Specification {
    @Canonical
    static class Fixture {
        @JsonSerialize(using= IntRangeJsonSerializer)
        @JsonDeserialize(using= IntRangeJsonDeserializer)
        Range<Integer> quantity
    }

    ObjectMapper mapper

    void setup() {
        mapper = new ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    def "range null"() {
        given:
        Fixture fixture = new Fixture()
        expect:
        mapper.readValue(mapper.writeValueAsString(fixture), Fixture) == fixture
    }

    def "range inclusive"() {
        given:
        Fixture fixture = new Fixture(quantity: 1..2)
        expect:
        mapper.readValue(mapper.writeValueAsString(fixture), Fixture) == fixture
    }

    def "range exclusive"() {
        given:
        Fixture fixture = new Fixture(quantity: 1..<2)
        expect:
        mapper.readValue(mapper.writeValueAsString(fixture), Fixture) == fixture
    }

    def "range reversed"() {
        given:
        Fixture fixture = new Fixture(quantity: 2..1)
        expect:
        mapper.readValue(mapper.writeValueAsString(fixture), Fixture) == fixture
    }
}

package org.patdouble.adventuregame.storage.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import groovy.transform.CompileStatic

/**
 * De-serializes an integer range from JSON.
 */
@CompileStatic
class IntRangeJsonDeserializer extends StdDeserializer<Range<Integer>> {

    IntRangeJsonDeserializer() {
        this(Range)
    }

    IntRangeJsonDeserializer(Class<?> vc) {
        super(vc)
    }

    @Override
    Range<Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = (JsonNode) p.getCodec().readTree(p)
        int from = ((IntNode) node.get('from')).numberValue().intValue()
        int to = ((IntNode) node.get('to')).numberValue().intValue()
        boolean inclusive = ((BooleanNode) node.get('incl')).booleanValue()

        if (inclusive) {
            return new IntRange(true, from, to)
        }
        return new IntRange(from, to)
    }

}

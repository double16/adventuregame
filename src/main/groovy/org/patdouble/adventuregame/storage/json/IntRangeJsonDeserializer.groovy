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
    public static final String NODE_REV = 'rev'
    public static final String NODE_INCL = 'incl'
    public static final String NODE_TO = 'to'
    public static final String NODE_FROM = 'from'

    IntRangeJsonDeserializer() {
        this(Range)
    }

    IntRangeJsonDeserializer(Class<?> vc) {
        super(vc)
    }

    @Override
    Range<Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = (JsonNode) p.getCodec().readTree(p)
        int from = ((IntNode) node.get(NODE_FROM)).numberValue().intValue()
        int to = ((IntNode) node.get(NODE_TO)).numberValue().intValue()
        boolean inclusive = ((BooleanNode) node.get(NODE_INCL)).booleanValue()
        boolean reverse = node.has(NODE_REV) ? ((BooleanNode) node.get(NODE_REV)).booleanValue() : false

        if (reverse) {
            int t = from
            from = to
            to = t
        }

        if (inclusive) {
            return new IntRange(true, from, to)
        }
        return new IntRange(from, to)
    }

}

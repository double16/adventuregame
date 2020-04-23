package org.patdouble.adventuregame.storage.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import groovy.transform.CompileStatic

/**
 * Serializes an integer range to JSON.
 */
@CompileStatic
class IntRangeJsonSerializer extends StdSerializer<Range<Integer>> {

    IntRangeJsonSerializer() {
        this(Range)
    }

    IntRangeJsonSerializer(Class<?> vc) {
        super(vc)
    }

    @Override
    void serialize(Range<Integer> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject()
        gen.writeNumberField(IntRangeJsonDeserializer.NODE_FROM, value.from)
        gen.writeNumberField(IntRangeJsonDeserializer.NODE_TO, value.to)
        gen.writeBooleanField(IntRangeJsonDeserializer.NODE_INCL, (value as IntRange).inclusive)
        if (value.reverse) {
            gen.writeBooleanField(IntRangeJsonDeserializer.NODE_REV, value.reverse)
        }
        gen.writeEndObject()
    }

}

package org.patdouble.adventuregame

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch

import java.time.Duration

/**
 * Helper methods for specs.
 */
class SpecHelper {
    static void settle(int retries = 3, Duration delay = Duration.ofSeconds(3), Closure dataHash) {
        Object hash = dataHash.call()
        while (retries-- > 1) {
            Thread.sleep(delay.toMillis())
            Object nextHash = dataHash.call()
            if (nextHash == hash) {
                break
            }
            hash = nextHash
        }
    }

    private static final ObjectMapper mapper
    static {
        mapper = new ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    static String unifiedDiffJson(Object original, Object revised, int contextSize = 3) {
        unifiedDiff(mapper.writeValueAsString(original), mapper.writeValueAsString(revised))
    }

    private static final JSON_ID_SEARCH = /"id"(\s*:\s*)"(.*?)"/
    private static final JSON_ID_REPLACEMENT = '"id"$1null'
    static String unifiedDiff(String original, String revised, int contextSize = 3) {
        List<String> originalLines = original.replaceAll(JSON_ID_SEARCH, JSON_ID_REPLACEMENT).split(/[\r\n]+/)
        List<String> revisedLines = revised.replaceAll(JSON_ID_SEARCH, JSON_ID_REPLACEMENT).split(/[\r\n]+/)
        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines)
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff('a.json', 'b.json', originalLines, patch, contextSize)
        unifiedDiff.join('\n')
    }
}

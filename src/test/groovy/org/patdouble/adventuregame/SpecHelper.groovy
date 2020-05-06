package org.patdouble.adventuregame

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
}

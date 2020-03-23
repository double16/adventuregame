package org.patdouble.adventuregame

import java.time.Duration

/**
 * Helper methods for specs.
 */
class SpecHelper {
    static void wait(int retries = 3, Duration delay = Duration.ofSeconds(1), Closure assertions) {
        while (retries-- > 1) {
            try {
                assertions.call()
                return
            } catch (Exception | AssertionError e) {
                // eat exceptions
            }
            Thread.sleep(delay.toMillis())
        }
        assertions.call()
    }
}

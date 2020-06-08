package org.patdouble.adventuregame.model

import java.security.MessageDigest

/** Identifies an object that can contribute to a secure hashing. */
trait CanSecureHash {
    static final String HASH_ALGORITHM = 'SHA-256'

    /** Update the hash with this object. */
    abstract void update(MessageDigest md)

    /**
     * Compute the secure hash for this object.
     */
    String computeSecureHash() {
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM)
        update(md)
        md.digest().encodeHex()
    }
}

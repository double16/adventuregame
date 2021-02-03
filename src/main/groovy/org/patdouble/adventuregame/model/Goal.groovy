package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import java.security.MessageDigest

/**
 * Identifies a condition to be met to guide actions of players and signal the end of the story.
 */
@Canonical(excludes = [Constants.COL_DBID], includePackage = false)
@Entity
@CompileDynamic
class Goal implements CanSecureHash {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    String name
    @Lob
    String description
    /** A required goal must be fulfilled before the story is over. */
    boolean required
    /** If this goal is met, end the story regardless of other goals. */
    boolean theEnd
    /** The rules for fulfilling the goal, written using the DSL (default.dsl). */
    @ElementCollection(fetch = FetchType.EAGER)
    List<String> rules = []

    @Override
    void update(MessageDigest md) {
        md.update((name ?: '').bytes)
        md.update((description ?: '').bytes)
        md.update(required ? (byte) 0x1 : (byte) 0x0)
        md.update(theEnd ? (byte) 0x1 : (byte) 0x0)
        rules.each { md.update(it.bytes) }
    }
}

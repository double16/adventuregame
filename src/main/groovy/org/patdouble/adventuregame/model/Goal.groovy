package org.patdouble.adventuregame.model

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

/**
 * Identifies a condition to be met to guide actions of players and signal the end of the story.
 */
@Canonical(excludes = [Constants.COL_ID], includePackage = false)
@Entity
@CompileDynamic
class Goal {
    @Id
    UUID id = UUID.randomUUID()

    String name
    /** A required goal must be fulfilled before the story is over. */
    boolean required
    /** If this goal is met, end the story regardless of other goals. */
    boolean theEnd
    /** The rules for fulfilling the goal, written using the DSL (rules/default.dsl). */
    @ElementCollection
    List<String> rules = []
}

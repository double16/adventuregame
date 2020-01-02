package org.patdouble.adventuregame.model

import groovy.transform.Canonical

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

/**
 * Identifies a condition to be met to guide actions of players and signal the end of the story.
 */
@Canonical(excludes = ['id'])
@Entity
class Goal {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    String name
    /** A required goal must be fulfilled before the story is over. */
    boolean required
    /** If this goal is met, end the story regardless of other goals. */
    boolean theEnd
}

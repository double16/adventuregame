package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.validation.constraints.NotNull

/**
 * Improvements:
 *
 * Personas could have different results for actions. For example, a thief could be better at fleeing than a warrior.
 * A warrior would take less damage during fighting. The thief could find more wealth on enemies after they are
 * defeated.
 */
@Canonical(excludes = [Constants.COL_ID, Constants.COL_DBID])
@AutoClone(excludes = [Constants.COL_ID, Constants.COL_DBID], style = AutoCloneStyle.COPY_CONSTRUCTOR)
@Entity
@CompileDynamic
class Persona {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @NotNull
    String name
    /** 0-1000, 0 is dead. */
    int health
    BigDecimal wealth
    /** 0-1000: 500 is neutral, <500 is 'bad', 0 is pure evil. */
    int virtue
    /** 0-1000: 500 is neutral, <500 is coward, >500 is courage. */
    int bravery
    /** 0-1000. */
    int leadership
    /** 0-1000. */
    int experience
    /** 0-1000. */
    int agility
    /** 0-1000. */
    int speed
    /** 0-1000. */
    int memory

    @Override
    String toString() {
        name
    }
}

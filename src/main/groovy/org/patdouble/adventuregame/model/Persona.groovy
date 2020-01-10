package org.patdouble.adventuregame.model

import groovy.transform.AutoClone
import groovy.transform.Canonical
import groovy.transform.CompileDynamic

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
@Canonical(excludes = ['id'])
@AutoClone
@Entity
@CompileDynamic
class Persona {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

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

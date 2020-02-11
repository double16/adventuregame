package org.patdouble.adventuregame.state

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.kie.api.definition.type.PropertyReactive
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Tracks fulfillment of a {@link Goal}.
 */
@Canonical(excludes = [Constants.COL_ID])
@Entity
@PropertyReactive
@CompileDynamic
class GoalStatus {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id
    @ManyToOne
    Goal goal
    boolean fulfilled
}

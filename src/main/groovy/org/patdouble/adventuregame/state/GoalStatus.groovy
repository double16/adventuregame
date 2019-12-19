package org.patdouble.adventuregame.state

import groovy.transform.Canonical
import org.kie.api.definition.type.PropertyReactive
import org.patdouble.adventuregame.model.Goal

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Tracks fulfillment of a {@link Goal}.
 */
@Canonical(excludes = ['id'])
@Entity
@PropertyReactive
class GoalStatus {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id
    @ManyToOne
    Goal goal
    boolean fulfilled
}

package org.patdouble.adventuregame.state

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Tracks fulfillment of a {@link Goal}.
 */
@Canonical(excludes = [Constants.COL_DBID, 'story'], includePackage = false)
@Entity
@CompileDynamic
class GoalStatus implements KieMutableProperties {
    private static final String[] KIE_MUTABLE_PROPS = [ 'fulfilled' ]

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()
    @ManyToOne
    @JsonIgnore
    Story story
    @ManyToOne(fetch = FetchType.EAGER)
    Goal goal
    boolean fulfilled

    @Override
    String[] kieMutableProperties() {
        KIE_MUTABLE_PROPS
    }
}

package org.patdouble.adventuregame.state.request

import groovy.transform.Canonical
import groovy.transform.ToString
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.state.Player

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * Requests the player to perform an action, i.e. a single 'move'.
 */
@Canonical(excludes = ['actions', 'directions'])
@ToString(includePackage = false, includeNames = true)
@Entity
class ActionRequest extends Request {
    @ManyToOne
    Player player
    /** The chronos value that will be fulfilled by the action. */
    @Column(insertable = false, updatable = false)
    long chronos
    @Embedded
    RoomSummary roomSummary
    /** The valid actions, may be a subset of the total. */
    @ElementCollection
    List<String> actions = []
    /** The visible directions to neighbors. */
    @ElementCollection
    List<String> directions = []
}

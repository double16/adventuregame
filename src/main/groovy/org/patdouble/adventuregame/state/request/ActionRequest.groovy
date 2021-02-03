package org.patdouble.adventuregame.state.request

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.ToString
import org.hibernate.Hibernate
import org.hibernate.annotations.Type
import org.patdouble.adventuregame.flow.RoomSummary
import org.patdouble.adventuregame.state.Player

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.ManyToOne

/**
 * Requests the player to perform an action, i.e. a single 'move'.
 */
@Canonical(excludes = ['actions', 'directions'])
@ToString(includePackage = false, includeNames = true)
@Entity
@CompileDynamic
class ActionRequest extends Request {
    @ManyToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH])
    Player player
    /** The chronos value that will be fulfilled by the action. */
    @Column(insertable = false, updatable = false)
    long chronos
    @Embedded
    RoomSummary roomSummary
    /** The valid actions, may be a subset of the total. */
    @Type(type = 'org.patdouble.adventuregame.storage.jpa.DelimitedStringListUserType')
    @Column(length = 2048)
    List<String> actions = []
    /** The visible directions to neighbors. */
    @Type(type = 'org.patdouble.adventuregame.storage.jpa.DelimitedStringListUserType')
    @Column(length = 2048)
    List<String> directions = []

    @Override
    ActionRequest initialize() {
        super.initialize()
        Hibernate.initialize(player)
        player.initialize()
        Hibernate.initialize(actions)
        Hibernate.initialize(directions)
        this
    }
}

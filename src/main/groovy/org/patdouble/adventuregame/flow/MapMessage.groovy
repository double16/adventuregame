package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString
import org.patdouble.adventuregame.state.Player

/**
 * Represents the map in a shortened form.
 */
@Canonical
@ToString(includePackage = false, includeNames = true)
@CompileStatic
class MapMessage extends StoryMessage {
    @Immutable(includePackage = false, includeNames = true)
    static class RoomInfo {
        String id
        String name
    }

    @Immutable(includePackage = false, includeNames = true)
    static class RoomEdgeInfo {
        /** References {@link RoomInfo#id} */
        String from
        /** References {@link RoomInfo#id} */
        String to
        String direction
    }

    Player player
    Set<RoomInfo> rooms = new HashSet<>()
    Set<RoomEdgeInfo> edges = new HashSet<>()
}

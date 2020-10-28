package org.patdouble.adventuregame.flow

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString
import org.patdouble.adventuregame.state.Player

import javax.validation.constraints.NotNull

/**
 * Represents the map in a shortened form.
 */
@Canonical
@ToString(includePackage = false, includeNames = true)
@CompileStatic
class MapMessage extends StoryMessage {
    @Immutable(includePackage = false, includeNames = true)
    static class RegionInfo {
        @NotNull
        String id
        @NotNull
        String name
        String parent
    }

    @Immutable(includePackage = false, includeNames = true)
    static class RoomInfo {
        @NotNull
        String id
        @NotNull
        String name
        String region
    }

    @Immutable(includePackage = false, includeNames = true)
    static class RoomEdgeInfo {
        /** References {@link RoomInfo#id} */
        @NotNull
        String from
        /** References {@link RoomInfo#id} */
        @NotNull
        String to
        @NotNull
        String direction
        /** Indicates the opposite direction in a two way path (which is the most common). Ex: north/south. */
        String back

        @Override
        int hashCode() {
            ([from, to, direction, back].findAll() as SortedSet).hashCode()
        }

        @Override
        boolean equals(Object obj) {
            if (!(obj instanceof RoomEdgeInfo)) {
                return false
            }
            RoomEdgeInfo other = (RoomEdgeInfo) obj
            if (from == other.from && to == other.to && direction == other.direction && back == other.back) {
                return true
            }
            if (from == other.to && to == other.from && direction == other.back && back == other.direction) {
                return true
            }
            return false
        }
    }

    @NotNull
    Player player
    @NotNull
    Set<RegionInfo> regions = [] as Set
    @NotNull
    Set<RoomInfo> rooms = [] as Set
    @NotNull
    Set<RoomEdgeInfo> edges = [] as Set
    /** The map in GraphViz 'dot' format */
    String dot
}

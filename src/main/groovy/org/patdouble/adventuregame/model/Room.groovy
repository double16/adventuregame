package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.validation.constraints.NotNull

/**
 * A place of non-deterministic size that can hold objects and players.
 */
@Entity
@ToString(excludes = ['dbId', 'neighbors'])
class Room {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long dbId

    String id
    String name
    String description

    /** Maps a direction to a room. */
    @ManyToMany
    @JsonIgnore
    Map<String, Room> neighbors = new HashMap<>()

    Map<String, Room> getNeighbors() {
        Collections.unmodifiableMap(neighbors)
    }

    void addNeighbor(@NotNull String direction, @NotNull Room room) {
        assert direction != null
        assert room != null
        direction = direction.toLowerCase()
        if (neighbors.containsKey(direction)) {
            throw new IllegalArgumentException("Neighbor in direction ${direction} already present: ${neighbors.get(direction)}")
        }
        neighbors.put(direction, room)
    }

    @Override
    int hashCode() {
        Objects.hash(id, name)
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof Room)) {
            return false
        }
        Room r2 = (Room) obj
        if (id != r2.id) {
            return false
        }
        if (name != r2.name) {
            return false
        }
        if (description != r2.description) {
            return false
        }
        if (neighbors.keySet() != r2.neighbors.keySet()) {
            return false
        }
        return !neighbors.any { k,v -> v.id != r2.neighbors.get(k).id }
    }
}

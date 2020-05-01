package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.ToString
import org.hibernate.Hibernate
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.ManyToMany
import javax.validation.constraints.NotNull

/**
 * A place of non-deterministic size that can hold objects and players.
 */
@Entity
@ToString(excludes = [Constants.COL_ID, Constants.COL_DBID, 'neighbors'], includePackage = false)
@CompileDynamic
class Room {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    String modelId
    String name
    @Lob
    String description

    /** Maps a direction to a room. */
    @ManyToMany
    @JsonIgnore
    Map<String, Room> neighbors = [:]

    Map<String, Room> getNeighbors() {
        Collections.unmodifiableMap(neighbors)
    }

    @SuppressWarnings('ParameterReassignment')
    void addNeighbor(@NotNull String direction, @NotNull Room room) {
        assert direction != null
        assert room != null
        direction = direction.toLowerCase()
        if (neighbors.containsKey(direction)) {
            throw new IllegalArgumentException(
                    "Neighbor in direction ${direction} already present: ${neighbors.get(direction)}")
        }
        neighbors.put(direction, room)
    }

    @Override
    int hashCode() {
        Objects.hash(modelId, name)
    }

    @Override
    @SuppressWarnings('Instanceof')
    boolean equals(Object obj) {
        if (!(obj instanceof Room)) {
            return false
        }
        Room r2 = (Room) obj
        if (modelId != r2.modelId) {
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
        return !neighbors.any { k, v -> v.modelId != r2.neighbors.get(k).modelId }
    }

    Room initialize() {
        Hibernate.initialize(neighbors)
        this
    }
}

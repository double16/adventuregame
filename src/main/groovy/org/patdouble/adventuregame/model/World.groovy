package org.patdouble.adventuregame.model

import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.hibernate.Hibernate
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.OneToMany
import javax.persistence.PrePersist
import java.security.MessageDigest

/**
 * Models the world for the starting point of one or more stories.
 *
 * This is the top level model object.
 */
@Entity
@EqualsAndHashCode(excludes = [Constants.COL_ID])
@ToString(includePackage = false, includes = ['id', 'name'])
@CompileDynamic
class World implements CanSecureHash {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    String name
    String author
    @Lob
    String description

    /** Only one active World with the given {@link #name}. */
    boolean active = false
    /** Monotonically increasing edition. */
    int edition
    /** The hash for this World, not from {@link #hashCode()}. */
    String hash

    @OneToMany(cascade = CascadeType.ALL)
    List<Persona> personas = []

    /**
     * List of available players.
     */
    @OneToMany(cascade = CascadeType.ALL)
    List<PlayerTemplate> players = []

    /**
     * List of extras, which are like players but are only controlled by AI.
     */
    @OneToMany(cascade = CascadeType.ALL)
    List<ExtrasTemplate> extras = []

    @OneToMany(cascade = CascadeType.ALL)
    List<Region> regions = []

    @OneToMany(cascade = CascadeType.ALL)
    List<Room> rooms = []

    @OneToMany(cascade = CascadeType.ALL)
    List<Goal> goals = []

    Optional<Room> findRoomById(String id) {
        Objects.requireNonNull(id)
        getRooms().stream().filter { it.modelId == id }.findFirst()
    }

    Optional<Region> findRegionById(String id) {
        Objects.requireNonNull(id)
        getRegions().stream().filter { it.modelId == id }.findFirst()
    }

    Collection<Room> findRoomsByRegion(Region region) {
        Objects.requireNonNull(region)
        Set<String> regionIds = regions.collect { it.parentageModelIds }
               .findAll { it.contains(region.modelId) }
               .collect { it.subList(0, it.indexOf(region.modelId)+1) }
               .flatten() as Set
        getRooms().findAll { it.region?.modelId in regionIds }
    }

    World initialize() {
        Hibernate.initialize(personas)
        Hibernate.initialize(players)
        players*.initialize()
        Hibernate.initialize(extras)
        extras*.initialize()
        Hibernate.initialize(regions)
        regions*.initialize()
        Hibernate.initialize(rooms)
        rooms*.initialize()
        Hibernate.initialize(goals)
        this
    }

    @Override
    void update(MessageDigest md) {
        md.update((name ?: "").bytes)
        md.update((author ?: "").bytes)
        md.update((description ?: "").bytes)
        personas*.update(md)
        players*.update(md)
        extras*.update(md)
        regions*.update(md)
        rooms*.update(md)
        goals*.update(md)
    }

    @PrePersist
    @SuppressWarnings('Unused')
    void prePersist() {
        if (!hash) {
            hash = computeSecureHash()
        }
    }
}

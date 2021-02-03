package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileDynamic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.hibernate.Hibernate
import org.patdouble.adventuregame.state.KieMutableProperties
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull
import java.security.MessageDigest

/**
 * Identifies a geographical or geopolitical region, which in effect is a group of rooms.
 */
@Entity
@ToString(excludes = [Constants.COL_ID, Constants.COL_DBID, 'parent'], includePackage = false)
@EqualsAndHashCode(excludes = [Constants.COL_ID, Constants.COL_DBID])
@CompileDynamic
class Region implements KieMutableProperties, CanSecureHash {
    private static final String[] KIE_MUTABLE_PROPS = []

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @NotNull
    String modelId
    @NotNull
    String name
    @Lob
    String description
    @ManyToOne
    Region parent

    @Override
    String[] kieMutableProperties() {
        KIE_MUTABLE_PROPS
    }

    Region initialize() {
        Hibernate.initialize(parent)
        this
    }

    /**
     * Get the list of regions starting with this region and ending with the top level region.
     */
    List<String> getParentageModelIds() {
        List<String> s = []
        Region r = this
        while (r) {
            s << r.modelId
            r = r.parent
        }
        s
    }

    @Override
    void update(MessageDigest md) {
        md.update((modelId ?: '').bytes)
        md.update((name ?: '').bytes)
        md.update((description ?: '').bytes)
        if (parent) {
            md.update(parent.name.bytes)
        }
    }
}

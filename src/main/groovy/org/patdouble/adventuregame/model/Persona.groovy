package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.patdouble.adventuregame.state.KieMutableProperties
import org.patdouble.adventuregame.storage.jpa.Constants

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.validation.constraints.NotNull
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Improvements:
 *
 * Personas could have different results for actions. For example, a thief could be better at fleeing than a warrior.
 * A warrior would take less damage during fighting. The thief could find more wealth on enemies after they are
 * defeated.
 */
@Canonical(excludes = [Constants.COL_ID, Constants.COL_DBID])
@AutoClone(excludes = [Constants.COL_ID, Constants.COL_DBID], style = AutoCloneStyle.COPY_CONSTRUCTOR)
@Entity
@Builder(builderStrategy = SimpleStrategy, excludes = [Constants.COL_ID, Constants.COL_DBID])
@CompileDynamic
class Persona implements KieMutableProperties, CanSecureHash {
    static final String[] KIE_MUTABLE_PROPS = ['health', 'wealth', 'virtue', 'bravery', 'leadership', 'experience', 'agility', 'speed', 'memory']
    private static final int ATTR_MAX_LEVEL = 1000
    private static final int ATTR_MIN_LEVEL = 0
    private static final int ATTR_MID_LEVEL = 500

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @NotNull
    String name
    /** 0-1000, 0 is dead. */
    int health = ATTR_MAX_LEVEL
    BigDecimal wealth = BigDecimal.ZERO.setScale(2)
    /** 0-1000: 500 is neutral, <500 is 'bad', 0 is pure evil. */
    int virtue = ATTR_MID_LEVEL
    /** 0-1000: 500 is neutral, <500 is coward, >500 is courage. */
    int bravery = ATTR_MID_LEVEL
    /** 0-1000. */
    int leadership = ATTR_MID_LEVEL
    /** 0-1000. */
    int experience = ATTR_MIN_LEVEL
    /** 0-1000. */
    int agility = ATTR_MID_LEVEL
    /** 0-1000. */
    int speed = ATTR_MID_LEVEL
    /** 0-1000. */
    int memory = ATTR_MID_LEVEL

    @Override
    String[] kieMutableProperties() {
        KIE_MUTABLE_PROPS
    }

    @Override
    String toString() {
        name
    }

    @Override
    void update(MessageDigest md) {
        ByteBuffer intb = ByteBuffer.allocate(4)
        md.update(name.bytes)
        md.update(intb.putInt(health).array())
        md.update(wealth.unscaledValue().toByteArray())
        md.update(intb.rewind().putInt(virtue).array())
        md.update(intb.rewind().putInt(bravery).array())
        md.update(intb.rewind().putInt(leadership).array())
        md.update(intb.rewind().putInt(experience).array())
        md.update(intb.rewind().putInt(agility).array())
        md.update(intb.rewind().putInt(speed).array())
        md.update(intb.rewind().putInt(memory).array())
    }
}

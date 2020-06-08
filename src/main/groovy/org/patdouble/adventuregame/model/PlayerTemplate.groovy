package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.hibernate.annotations.Columns
import org.hibernate.annotations.Type
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.storage.jpa.Constants
import org.patdouble.adventuregame.storage.json.IntRangeJsonDeserializer
import org.patdouble.adventuregame.storage.json.IntRangeJsonSerializer

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.validation.constraints.NotNull
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Template for players in the story. Players can either be human or AI.
 */
@Canonical(includes = [Constants.COL_ID, Constants.COL_DBID])
@Entity
@CompileDynamic
class PlayerTemplate implements CharacterTrait {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    /** The allowed quantity of this type of player. */
    @Type(type = 'org.patdouble.adventuregame.storage.jpa.IntRangeUserType')
    @Columns(columns = [ @Column(name = 'QTY_FROM'), @Column(name = 'QTY_TO'), @Column(name = 'QTY_INCL') ])
    @JsonSerialize(using= IntRangeJsonSerializer)
    @JsonDeserialize(using= IntRangeJsonDeserializer)
    Range<Integer> quantity = 1..1

    Player createPlayer(@NotNull Motivator motivator) {
        Objects.requireNonNull(motivator)
        CharacterTrait.super.createPlayer(motivator)
    }

    @Override
    @JsonIgnore
    Persona getPersona() {
        CharacterTrait.super.getPersona()
    }

    @Override
    String toString() {
        "PlayerTemplate: ${persona.toString()}, id ${id}, nick ${nickName}, " +
                "full ${fullName}, room ${room?.modelId}, qty ${quantity.toString()}"
    }

    @SuppressWarnings('Instanceof')
    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof PlayerTemplate)) {
            return false
        }
        PlayerTemplate other = obj
        return Objects.equals(quantity, other.quantity) &&
                Objects.equals(persona, other.persona) &&
                Objects.equals(nickName, other.nickName) &&
                Objects.equals(fullName, other.fullName)
    }

    @Override
    int hashCode() {
        Objects.hash(quantity, persona, nickName, fullName)
    }

    @Override
    void update(MessageDigest md) {
        CharacterTrait.super.update(md)
        ByteBuffer intb = ByteBuffer.allocate(4)
        md.update(intb.rewind().putInt(quantity.fromInt).array())
        md.update(intb.rewind().putInt(quantity.toInt).array())
        md.update(quantity.isReverse() ? (byte) 0x1 : (byte) 0x0)
    }
}

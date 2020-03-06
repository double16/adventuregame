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

/**
 * Template for players in the story. Players can either be human or AI.
 */
@Canonical(excludes = [Constants.COL_ID])
@Entity
@CompileDynamic
class PlayerTemplate implements CharacterTrait {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

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
        "PlayerTemplate: ${persona.toString()}, nick ${nickName}, " +
                "full ${fullName}, room ${room?.id}, qty ${quantity.toString()}"
    }
}

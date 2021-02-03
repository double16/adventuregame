package org.patdouble.adventuregame.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.hibernate.Hibernate
import org.hibernate.annotations.Columns
import org.hibernate.annotations.Type
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.storage.jpa.Constants
import org.patdouble.adventuregame.storage.json.IntRangeJsonDeserializer
import org.patdouble.adventuregame.storage.json.IntRangeJsonSerializer

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderColumn
import javax.validation.constraints.NotNull
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Template for players in the story. Players can either be human or AI.
 */
@Canonical(includes = [Constants.COL_ID, Constants.COL_DBID])
@Entity
@CompileDynamic
class PlayerTemplate implements CanSecureHash {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @Delegate(excludes = [ 'clone', 'update', 'computeSecureHash', Constants.COL_DBID, Constants.COL_ID ])
    @ManyToOne
    @NotNull
    @JsonIgnore
    Persona persona = new Persona()

    String nickName

    String fullName

    @ManyToOne
    @NotNull
    Room room

    /** List of rooms that are always known to the player, i.e. long term memory. */
    @ManyToMany
    @JsonIgnore // this could be large
    List<Room> knownRooms = []

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name= 'INDEX')
    List<Goal> goals = []

    /** The allowed quantity of this type of player. */
    @Type(type = 'org.patdouble.adventuregame.storage.jpa.IntRangeUserType')
    @Columns(columns = [ @Column(name = 'QTY_FROM'), @Column(name = 'QTY_TO'), @Column(name = 'QTY_INCL') ])
    @JsonSerialize(using= IntRangeJsonSerializer)
    @JsonDeserialize(using= IntRangeJsonDeserializer)
    Range<Integer> quantity = 1..1

    Player createPlayer(@NotNull Motivator motivator) {
        Objects.requireNonNull(motivator)
        Player p = new Player(this, motivator, persona, nickName)
        p.fullName = fullName
        p.room = room
        p
    }

    List<Player> createPlayers() {
        List<Player> players = []
        int c = quantity.max()
        while (c-- > 0) {
            players << createPlayer(Motivator.AI)
        }
        players
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

    PlayerTemplate initialize() {
        Hibernate.initialize(persona)
        Hibernate.initialize(room)
        Hibernate.initialize(knownRooms)
        Hibernate.initialize(goals)
        this
    }

    @Override
    void update(MessageDigest md) {
        persona.update(md)
        md.update((nickName ?: '').bytes)
        md.update((fullName ?: '').bytes)
        md.update((room?.modelId ?: '').bytes)
        knownRooms.each { md.update(it.modelId.bytes) }
        goals*.update(md)

        ByteBuffer intb = ByteBuffer.allocate(4)
        md.update(intb.rewind().putInt(quantity.fromInt).array())
        md.update(intb.rewind().putInt(quantity.toInt).array())
        md.update(quantity.isReverse() ? (byte) 0x1 : (byte) 0x0)
    }
}

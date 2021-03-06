package org.patdouble.adventuregame.state

import groovy.transform.CompileStatic
import org.hibernate.annotations.CreationTimestamp

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.ManyToOne
import java.sql.Blob
import java.time.LocalDateTime

/**
 * Stores an agenda log generated by the Drools engine for the Story. This is used for debugging. The format is
 * compressed XML. The compression format is GZIP. The schema is determined by Drools.
 */
@Entity
@CompileStatic
class AgendaLog {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    @ManyToOne
    Story story
    @CreationTimestamp
    @SuppressWarnings('Unused')
    LocalDateTime created
    @Lob
    Blob gzlog

    @Override
    String toString() {
        "Agenda log for story ${story.id} at ${created}"
    }
}

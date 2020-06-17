package org.patdouble.adventuregame.state.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.state.Story

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Base class for requests for user input.
 */
@Entity
@CompileDynamic
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property= '@class')
class Request {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    UUID dbId
    /** 'business' id */
    UUID id = UUID.randomUUID()

    @ManyToOne
    @JsonIgnore
    Story story

    protected Request() { }

    Request initialize() {
        this
    }
}

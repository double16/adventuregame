package org.patdouble.adventuregame.state.request

import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileDynamic

import javax.persistence.Entity
import javax.persistence.Id

/**
 * Base class for requests for user input.
 */
@Entity
@CompileDynamic
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property= '@class')
class Request {
    @Id
    UUID id = UUID.randomUUID()

    protected Request() { }

    Request initialize() {
        this
    }
}

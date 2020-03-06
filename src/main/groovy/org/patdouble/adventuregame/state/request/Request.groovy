package org.patdouble.adventuregame.state.request

import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileDynamic

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

/**
 * Base class for requests for user input.
 */
@Entity
@CompileDynamic
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property= '@class')
class Request {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    protected Request() { }
}

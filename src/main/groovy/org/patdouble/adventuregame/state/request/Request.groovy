package org.patdouble.adventuregame.state.request

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
class Request {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id

    protected Request() { }
}

package org.patdouble.adventuregame.state.request

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
abstract class Request {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    protected Request() { }
}

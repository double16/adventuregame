package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

import java.security.Principal

/**
 * Principal when allowing anonymous users.
 */
@CompileStatic
@EqualsAndHashCode(includes = ['name'])
class AnonymousPrincipal implements Principal {
    String name
}

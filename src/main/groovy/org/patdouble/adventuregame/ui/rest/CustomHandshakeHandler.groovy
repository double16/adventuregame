package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

import java.security.Principal

/**
 * Allows anonymous users.
 */
@CompileStatic
class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Principal principal = request.principal

        if (principal == null) {
            principal = new AnonymousPrincipal()

            String uniqueName = UUID.randomUUID()

            ((AnonymousPrincipal) principal).name = uniqueName
        }

        principal
    }
}

package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configuration for websockets.
 */
@Configuration
@EnableWebSocketMessageBroker
@CompileDynamic
@SuppressWarnings('UnnecessarySetter')
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    static final String TOPIC_PREFIX = '/topic'
    static final String USER_PREFIX = '/user'

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint('/socket').setHandshakeHandler(new CustomHandshakeHandler())
    }

    @Override
    void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(TOPIC_PREFIX)
        config.setApplicationDestinationPrefixes(TOPIC_PREFIX)
        config.setUserDestinationPrefix(USER_PREFIX)
    }
}

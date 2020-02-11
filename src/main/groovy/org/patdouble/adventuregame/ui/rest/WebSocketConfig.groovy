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
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint('/socket').withSockJS()
    }

    @Override
    void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker('/topic/', '/queue/')
        config.setApplicationDestinationPrefixes('/app/')
    }
}

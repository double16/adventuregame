package org.patdouble.adventuregame.engine

import groovy.transform.CompileStatic
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.MessagingException
import org.springframework.messaging.core.AbstractMessageSendingTemplate
import org.springframework.messaging.core.MessagePostProcessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.util.Assert
import org.springframework.util.StringUtils

/**
 * Records all messages sent.
 */
@CompileStatic
class RecordingSimpMessageTemplate extends AbstractMessageSendingTemplate<String> implements SimpMessageSendingOperations {
    List<Tuple2<String, Message>> messages = []

    @Override
    protected void doSend(String destination, Message<?> message) {
        messages << new Tuple2<String, Message>(destination, message)
    }

    @Override
    void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
        convertAndSendToUser(user, destination, payload, null, null)
    }

    @Override
    void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers) throws MessagingException {
        convertAndSendToUser(user, destination, payload, headers, null)
    }

    @Override
    void convertAndSendToUser(String user, String destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException {
        convertAndSendToUser(user, destination, payload, null, postProcessor)
    }

    @Override
    void convertAndSendToUser(String user, String destination, Object payload, @Nullable Map<String, Object> headers, @Nullable MessagePostProcessor postProcessor) throws MessagingException {
        Assert.notNull(user, "User must not be null")
        Assert.isTrue(!user.contains("%2F"), "Invalid sequence \"%2F\" in user name: " + user)
        user = StringUtils.replace(user, "/", "%2F")
        destination = destination.startsWith("/") ? destination : "/" + destination
        super.convertAndSend('/user/' + user + destination, payload, headers, postProcessor)
    }
}

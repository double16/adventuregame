package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.flow.StoryMessage
import org.springframework.messaging.simp.SimpMessageSendingOperations

import java.util.concurrent.Flow

/**
 * Forwards Flow messages to Spring Messaging.
 */
@CompileDynamic
@Slf4j
class EngineFlowToSpringMessagingAdapter implements Flow.Subscriber<StoryMessage> {

    private final SimpMessageSendingOperations simpMessagingTemplate
    private final UUID storyId
    private final String destination
    private Flow.Subscription subscription

    EngineFlowToSpringMessagingAdapter(UUID storyId, SimpMessageSendingOperations simpMessagingTemplate) {
        this.storyId = storyId
        this.destination = "/topic/story.${storyId}"
        this.simpMessagingTemplate = simpMessagingTemplate
    }

    @Override
    void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription
        subscription.request(Long.MAX_VALUE)
    }

    @Override
    void onNext(StoryMessage item) {
        // TODO: Do not forward messages related to AI
        log.debug 'Forwarding to {}: {}', destination, item
        simpMessagingTemplate.convertAndSend(destination, item, [type: item.class.simpleName])
        subscription.request(Long.MAX_VALUE)
    }

    @Override
    void onError(Throwable throwable) {
        log.error 'Eventing error', throwable
    }

    @Override
    void onComplete() {
        log.info 'Subscription completed'
    }
}

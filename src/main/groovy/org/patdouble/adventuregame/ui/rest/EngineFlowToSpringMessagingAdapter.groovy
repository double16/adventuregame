package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.flow.StoryMessage
import org.springframework.messaging.simp.SimpMessagingTemplate

import java.util.concurrent.Flow

/**
 * Forwards Flow messages to Spring Messaging.
 */
@CompileDynamic
@Slf4j
class EngineFlowToSpringMessagingAdapter implements Flow.Subscriber<StoryMessage> {

    private final SimpMessagingTemplate simpMessagingTemplate
    private final UUID storyId
    private final String destination
    private Flow.Subscription subscription

    EngineFlowToSpringMessagingAdapter(UUID storyId, SimpMessagingTemplate simpMessagingTemplate) {
        this.storyId = storyId
        this.destination = "/topic/story/${storyId}"
        this.simpMessagingTemplate = simpMessagingTemplate
    }

    @Override
    void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription
        subscription.request(Long.MAX_VALUE)
    }

    @Override
    void onNext(StoryMessage item) {
        log.info "Forwarding to ${destination}: ${item}"
        simpMessagingTemplate.convertAndSend(destination, item, [type: item.class.simpleName])
    }

    @Override
    void onError(Throwable throwable) {
        // nothing to do
    }

    @Override
    void onComplete() {
        // nothing to do
    }
}

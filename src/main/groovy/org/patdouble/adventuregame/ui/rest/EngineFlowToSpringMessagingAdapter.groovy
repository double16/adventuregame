package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.flow.StoryMessage
import org.springframework.messaging.simp.SimpMessagingTemplate

import java.util.concurrent.Flow

/**
 * Forwards Flow messages to Spring Messaging.
 */
@CompileDynamic
class EngineFlowToSpringMessagingAdapter implements Flow.Subscriber<StoryMessage> {

    private final SimpMessagingTemplate simpMessagingTemplate
    private final UUID storyId
    private Flow.Subscription subscription

    EngineFlowToSpringMessagingAdapter(UUID storyId, SimpMessagingTemplate simpMessagingTemplate) {
        this.storyId = storyId
        this.simpMessagingTemplate = simpMessagingTemplate
    }

    @Override
    void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription
        subscription.request(1)
    }

    @Override
    void onNext(StoryMessage item) {
        simpMessagingTemplate.convertAndSend("/topic/story/${storyId}", item)
        subscription.request(1)
    }

    @Override
    void onError(Throwable throwable) {
        subscription.request(1)
    }

    @Override
    void onComplete() {
        // nothing to do
    }
}

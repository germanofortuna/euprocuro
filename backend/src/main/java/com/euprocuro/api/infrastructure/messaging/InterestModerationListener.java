package com.euprocuro.api.infrastructure.messaging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.euprocuro.api.application.usecase.ModerationUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InterestModerationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterestModerationListener.class);

    private final ModerationUseCase moderationUseCase;

    @RabbitListener(queues = "${application.messaging.rabbit.interest-moderation-queue:euprocuro.interest.moderation}")
    public void handle(Map<String, Object> payload) {
        Object interestId = payload.get("interestId");
        if (interestId == null) {
            LOGGER.warn("Evento de moderacao ignorado: interestId ausente. Payload={}", payload);
            return;
        }

        moderationUseCase.processInterestModeration(String.valueOf(interestId));
    }
}

package com.euprocuro.api.infrastructure.realtime;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.model.ConversationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketRealtimeMessageGatewayAdapter implements RealtimeMessageGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketRealtimeMessageGatewayAdapter.class);
    private static final String CONVERSATION_MESSAGE_CREATED = "conversation-message.created";

    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void publishConversationMessage(String userId, ConversationMessage message) {
        try {
            RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                    .type(CONVERSATION_MESSAGE_CREATED)
                    .createdAt(Instant.now())
                    .payload(ConversationMessageRealtimePayload.from(message))
                    .build();

            sessionRegistry.sendToUser(userId, objectMapper.writeValueAsString(envelope));
        } catch (Exception exception) {
            LOGGER.warn("Nao foi possivel enviar mensagem em tempo real para usuario '{}'.", userId);
        }
    }
}

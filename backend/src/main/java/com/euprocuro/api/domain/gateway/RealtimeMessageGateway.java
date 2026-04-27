package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.ConversationMessage;

public interface RealtimeMessageGateway {
    void publishConversationMessage(String userId, ConversationMessage message);
}

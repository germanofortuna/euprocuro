package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.ConversationMessage;

public interface RealtimeMessageGateway {
    void publishConversationMessage(String userId, ConversationMessage message);

    void publishOfferCreated(String userId, String offerId);

    void publishInterestModerationUpdated(String userId, String interestId, String status, String reason);
}

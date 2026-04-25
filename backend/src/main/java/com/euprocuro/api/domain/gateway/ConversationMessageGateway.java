package com.euprocuro.api.domain.gateway;

import java.util.List;

import com.euprocuro.api.domain.model.ConversationMessage;

public interface ConversationMessageGateway {
    ConversationMessage save(ConversationMessage message);

    List<ConversationMessage> findByOfferIdOrderByCreatedAtAsc(String offerId);

    List<ConversationMessage> findByOfferIdInOrderByCreatedAtAsc(List<String> offerIds);
}

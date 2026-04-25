package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.ConversationMessageDocument;

public interface SpringDataConversationMessageRepository extends MongoRepository<ConversationMessageDocument, String> {
    List<ConversationMessageDocument> findByOfferIdOrderByCreatedAtAsc(String offerId);

    List<ConversationMessageDocument> findByOfferIdInOrderByCreatedAtAsc(List<String> offerIds);
}

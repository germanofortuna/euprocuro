package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.ConversationMessage;
import com.euprocuro.api.infrastructure.persistence.document.ConversationMessageDocument;

public final class ConversationMessagePersistenceMapper {

    private ConversationMessagePersistenceMapper() {
    }

    public static ConversationMessage toDomain(ConversationMessageDocument document) {
        if (document == null) {
            return null;
        }

        return ConversationMessage.builder()
                .id(document.getId())
                .offerId(document.getOfferId())
                .interestPostId(document.getInterestPostId())
                .senderId(document.getSenderId())
                .senderName(document.getSenderName())
                .recipientId(document.getRecipientId())
                .recipientName(document.getRecipientName())
                .content(document.getContent())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static ConversationMessageDocument toDocument(ConversationMessage domain) {
        if (domain == null) {
            return null;
        }

        return ConversationMessageDocument.builder()
                .id(domain.getId())
                .offerId(domain.getOfferId())
                .interestPostId(domain.getInterestPostId())
                .senderId(domain.getSenderId())
                .senderName(domain.getSenderName())
                .recipientId(domain.getRecipientId())
                .recipientName(domain.getRecipientName())
                .content(domain.getContent())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

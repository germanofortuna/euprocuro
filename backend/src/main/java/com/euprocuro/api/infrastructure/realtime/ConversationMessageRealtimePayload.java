package com.euprocuro.api.infrastructure.realtime;

import java.time.Instant;

import com.euprocuro.api.domain.model.ConversationMessage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConversationMessageRealtimePayload {
    String id;
    String offerId;
    String senderId;
    String senderName;
    String recipientId;
    String recipientName;
    String content;
    Instant createdAt;

    public static ConversationMessageRealtimePayload from(ConversationMessage message) {
        return ConversationMessageRealtimePayload.builder()
                .id(message.getId())
                .offerId(message.getOfferId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .recipientId(message.getRecipientId())
                .recipientName(message.getRecipientName())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

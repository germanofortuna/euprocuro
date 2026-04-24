package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConversationMessageResponse {
    String id;
    String offerId;
    String senderId;
    String senderName;
    String recipientId;
    String recipientName;
    String content;
    Instant createdAt;
}

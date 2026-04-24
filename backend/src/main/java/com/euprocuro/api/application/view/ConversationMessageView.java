package com.euprocuro.api.application.view;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConversationMessageView {
    String id;
    String offerId;
    String senderId;
    String senderName;
    String recipientId;
    String recipientName;
    String content;
    Instant createdAt;
}

package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {
    private String id;
    private String offerId;
    private String interestPostId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String recipientName;
    private String content;
    private Instant createdAt;
}

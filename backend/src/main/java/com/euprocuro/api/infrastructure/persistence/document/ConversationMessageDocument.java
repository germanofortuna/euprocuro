package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("conversation_messages")
public class ConversationMessageDocument {
    @Id
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

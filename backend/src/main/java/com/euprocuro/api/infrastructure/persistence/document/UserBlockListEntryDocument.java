package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("user_block_list")
public class UserBlockListEntryDocument {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String userEmail;

    @Indexed(unique = true)
    private String documentHash;

    private String documentLast4;

    private String documentType;

    @Indexed
    private boolean active;

    private String sourceProvider;
    private String sourceInterestId;
    private String reason;
    private int occurrenceCount;
    private Instant firstBlockedAt;
    private Instant lastBlockedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

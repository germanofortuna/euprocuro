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
public class UserBlockListEntry {
    private String id;
    private String userId;
    private String userEmail;
    private String documentHash;
    private String documentLast4;
    private String documentType;
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

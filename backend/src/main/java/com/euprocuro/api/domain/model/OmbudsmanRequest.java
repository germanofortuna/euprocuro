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
public class OmbudsmanRequest {
    private String id;
    private String protocol;
    private String userId;
    private String name;
    private String email;
    private String type;
    private String subject;
    private String message;
    private String relatedEntityType;
    private String relatedEntityId;
    private OmbudsmanRequestStatus status;
    private String adminResponse;
    private String answeredBy;
    private Instant answeredAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

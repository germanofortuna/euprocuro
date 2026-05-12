package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OmbudsmanRequestResponse {
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

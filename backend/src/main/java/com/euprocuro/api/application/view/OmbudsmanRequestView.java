package com.euprocuro.api.application.view;

import java.time.Instant;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmbudsmanRequestView {
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

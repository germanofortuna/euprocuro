package com.euprocuro.api.domain.model;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String id;
    private Instant occurredAt;
    private String action;
    private String actorUserId;
    private String actorEmail;
    private String resourceType;
    private String resourceId;
    private String outcome;
    private Map<String, Object> metadata;
}

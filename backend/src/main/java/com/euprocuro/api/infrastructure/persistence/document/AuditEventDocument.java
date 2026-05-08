package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("audit_events")
@CompoundIndexes({
        @CompoundIndex(name = "audit_actor_time_idx", def = "{'actorUserId': 1, 'occurredAt': -1}"),
        @CompoundIndex(name = "audit_resource_time_idx", def = "{'resourceType': 1, 'resourceId': 1, 'occurredAt': -1}"),
        @CompoundIndex(name = "audit_action_time_idx", def = "{'action': 1, 'occurredAt': -1}")
})
public class AuditEventDocument {
    @Id
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

package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("ombudsman_requests")
public class OmbudsmanRequestDocument {
    @Id
    private String id;

    @Indexed(unique = true)
    private String protocol;

    @Indexed
    private String userId;

    private String name;

    @Indexed
    private String email;

    private String type;
    private String subject;
    private String message;
    private String relatedEntityType;
    private String relatedEntityId;

    @Indexed
    private OmbudsmanRequestStatus status;

    private String adminResponse;
    private String answeredBy;
    private Instant answeredAt;
    private Instant closedAt;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;
}

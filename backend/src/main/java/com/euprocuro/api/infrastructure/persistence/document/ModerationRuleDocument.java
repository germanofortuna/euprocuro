package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("moderation_rules")
public class ModerationRuleDocument {
    @Id
    private String id;

    @Indexed(unique = true)
    private String term;

    private ModerationRiskLevel riskLevel;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

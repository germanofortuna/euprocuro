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
public class ModerationRule {
    private String id;
    private String term;
    private ModerationRiskLevel riskLevel;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

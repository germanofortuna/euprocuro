package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModerationRuleResponse {
    String id;
    String term;
    ModerationRiskLevel riskLevel;
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}

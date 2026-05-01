package com.euprocuro.api.application.view;

import java.time.Instant;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModerationRuleView {
    String id;
    String term;
    ModerationRiskLevel riskLevel;
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}

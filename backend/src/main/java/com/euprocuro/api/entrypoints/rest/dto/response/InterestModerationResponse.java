package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestModerationResponse {
    ModerationRiskLevel riskLevel;
    Map<String, Boolean> categories;
    Map<String, Double> scores;
    boolean reviewRequired;
    String provider;
    String reason;
    Instant checkedAt;
    String reviewedBy;
    Instant reviewedAt;
}

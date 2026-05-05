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
public class InterestModeration {
    private ModerationRiskLevel riskLevel;
    private Map<String, Boolean> categories;
    private boolean flagged;
    private Map<String, Double> scores;
    private boolean reviewRequired;
    private String provider;
    private String reason;
    private Instant checkedAt;
    private String reviewedBy;
    private Instant reviewedAt;
}

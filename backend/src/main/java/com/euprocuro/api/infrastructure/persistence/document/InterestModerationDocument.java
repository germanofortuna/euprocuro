package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InterestModerationDocument {
    private ModerationRiskLevel riskLevel;
    private List<String> categories;
    private Map<String, Double> scores;
    private boolean reviewRequired;
    private String provider;
    private String reason;
    private Instant checkedAt;
    private String reviewedBy;
    private Instant reviewedAt;
}

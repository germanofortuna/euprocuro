package com.euprocuro.api.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InterestPost {
    private String id;
    private String ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String referenceImageUrl;
    private InterestCategory category;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocationInfo location;
    private List<String> tags;
    private Integer desiredRadiusKm;
    private boolean acceptsNationwideOffers;
    private boolean boostEnabled;
    private Instant boostedUntil;
    private String preferredCondition;
    private String preferredContactMode;
    private InterestStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}

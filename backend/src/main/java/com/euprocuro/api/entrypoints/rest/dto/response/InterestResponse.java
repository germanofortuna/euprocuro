package com.euprocuro.api.entrypoints.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestResponse {
    String id;
    String ownerId;
    String ownerName;
    String title;
    String description;
    String referenceImageUrl;
    InterestCategory category;
    BigDecimal budgetMin;
    BigDecimal budgetMax;
    LocationResponse location;
    List<String> tags;
    Integer desiredRadiusKm;
    boolean acceptsNationwideOffers;
    boolean boostEnabled;
    Instant boostedUntil;
    String preferredCondition;
    String preferredContactMode;
    InterestStatus status;
    Instant createdAt;
    Instant updatedAt;
}

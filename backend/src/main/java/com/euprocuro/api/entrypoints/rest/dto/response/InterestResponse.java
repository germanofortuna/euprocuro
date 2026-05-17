package com.euprocuro.api.entrypoints.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.euprocuro.api.domain.model.InterestStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterestResponse {
    String id;
    String ownerId;
    String ownerName;
    String title;
    String description;
    String referenceImageUrl;
    String category;
    BigDecimal budgetMin;
    BigDecimal budgetMax;
    LocationResponse location;
    List<String> tags;
    StickerDetailsResponse stickerDetails;
    Integer desiredRadiusKm;
    boolean allowsWhatsappContact;
    String whatsappContact;
    Instant boostedUntil;
    String preferredCondition;
    String preferredContactMode;
    InterestStatus status;
    InterestModerationResponse moderation;
    Instant createdAt;
    Instant updatedAt;
    Instant expiresAt;
}

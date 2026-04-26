package com.euprocuro.api.entrypoints.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerItemResponse {
    String id;
    String ownerId;
    String ownerName;
    String title;
    String description;
    String referenceImageUrl;
    InterestCategory category;
    BigDecimal desiredPrice;
    LocationResponse location;
    List<String> tags;
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}

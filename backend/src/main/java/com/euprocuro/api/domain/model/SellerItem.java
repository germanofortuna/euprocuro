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
public class SellerItem {
    private String id;
    private String ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String referenceImageUrl;
    private InterestCategory category;
    private BigDecimal desiredPrice;
    private LocationInfo location;
    private List<String> tags;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

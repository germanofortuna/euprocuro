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
public class Offer {
    private String id;
    private String interestPostId;
    private String sellerId;
    private String sellerName;
    private String sellerEmail;
    private String sellerPhone;
    private BigDecimal offeredPrice;
    private String message;
    private String offerImageUrl;
    private boolean includesDelivery;
    private List<String> highlights;
    private OfferStatus status;
    private Instant createdAt;
}

package com.euprocuro.api.entrypoints.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.euprocuro.api.domain.model.OfferStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OfferResponse {
    String id;
    String interestPostId;
    String sellerId;
    String sellerName;
    String sellerEmail;
    String sellerPhone;
    BigDecimal offeredPrice;
    String message;
    boolean includesDelivery;
    List<String> highlights;
    OfferStatus status;
    Instant createdAt;
}

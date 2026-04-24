package com.euprocuro.api.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.euprocuro.api.domain.model.OfferStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardOfferView {
    String id;
    String interestPostId;
    String interestTitle;
    String buyerId;
    String sellerName;
    String sellerEmail;
    String sellerPhone;
    String buyerName;
    BigDecimal offeredPrice;
    String message;
    boolean includesDelivery;
    List<String> highlights;
    OfferStatus status;
    Instant createdAt;
}

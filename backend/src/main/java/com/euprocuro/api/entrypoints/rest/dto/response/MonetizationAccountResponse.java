package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonetizationAccountResponse {
    Integer sellerCredits;
    Integer purchasedCreditsTotal;
    String subscriptionPlan;
    Instant subscriptionActiveUntil;
    boolean subscriptionActive;
    List<MonetizationProductResponse> products;
}

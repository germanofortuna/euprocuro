package com.euprocuro.api.application.view;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonetizationAccountView {
    Integer sellerCredits;
    Integer purchasedCreditsTotal;
    String subscriptionPlan;
    Instant subscriptionActiveUntil;
    boolean subscriptionActive;
    List<MonetizationProductView> products;
}

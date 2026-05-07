package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonetizationSettingsView {
    boolean creditPurchasesEnabled;
    boolean boostPurchasesEnabled;
}

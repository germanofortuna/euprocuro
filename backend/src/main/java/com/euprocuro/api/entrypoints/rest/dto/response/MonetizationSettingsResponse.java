package com.euprocuro.api.entrypoints.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonetizationSettingsResponse {
    boolean creditPurchasesEnabled;
    boolean boostPurchasesEnabled;
}

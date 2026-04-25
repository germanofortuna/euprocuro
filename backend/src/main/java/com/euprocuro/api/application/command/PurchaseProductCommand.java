package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PurchaseProductCommand {
    String productCode;
    String paymentMethod;
}

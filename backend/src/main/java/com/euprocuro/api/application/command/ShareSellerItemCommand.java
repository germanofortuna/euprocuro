package com.euprocuro.api.application.command;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShareSellerItemCommand {
    BigDecimal offeredPrice;
    String sellerPhone;
    String message;
    boolean includesDelivery;
}

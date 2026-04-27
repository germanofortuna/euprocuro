package com.euprocuro.api.application.command;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateOfferCommand {
    BigDecimal offeredPrice;
    String sellerPhone;
    String message;
    String offerImageUrl;
    boolean includesDelivery;
    List<String> highlights;
}

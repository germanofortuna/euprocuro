package com.euprocuro.api.entrypoints.rest.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OfferConversationResponse {
    String offerId;
    String interestPostId;
    String interestTitle;
    String buyerId;
    String buyerName;
    String sellerId;
    String sellerName;
    String sellerEmail;
    String sellerPhone;
    BigDecimal offeredPrice;
    List<ConversationMessageResponse> messages;
}

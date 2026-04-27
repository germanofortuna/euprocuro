package com.euprocuro.api.application.view;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OfferConversationView {
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
    String offerImageUrl;
    List<ConversationMessageView> messages;
}

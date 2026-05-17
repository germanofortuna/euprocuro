package com.euprocuro.api.application.command;

import java.math.BigDecimal;
import java.util.List;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateInterestCommand {
    String title;
    String description;
    String referenceImageUrl;
    String category;
    BigDecimal budgetMin;
    BigDecimal budgetMax;
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
    Integer desiredRadiusKm;
    StickerDetailsCommand stickerDetails;
    boolean allowsWhatsappContact;
    String whatsappContact;
    String preferredCondition;
    String preferredContactMode;
    List<String> tags;
}

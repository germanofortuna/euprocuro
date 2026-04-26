package com.euprocuro.api.application.command;

import java.math.BigDecimal;
import java.util.List;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UpdateInterestCommand {
    String title;
    String description;
    String referenceImageUrl;
    InterestCategory category;
    BigDecimal budgetMin;
    BigDecimal budgetMax;
    String city;
    String state;
    String neighborhood;
    Integer desiredRadiusKm;
    boolean acceptsNationwideOffers;
    boolean allowsWhatsappContact;
    String whatsappContact;
    boolean boostEnabled;
    String preferredCondition;
    String preferredContactMode;
    List<String> tags;
}

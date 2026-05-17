package com.euprocuro.api.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestSearchCriteria {
    String category;
    String city;
    String state;
    String neighborhood;
    BigDecimal maxBudget;
    String query;
    String stickerType;
    String stickerGroup;
    String stickerSelection;
    String stickerNumber;
    boolean openOnly;
}

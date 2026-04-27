package com.euprocuro.api.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestSearchCriteria {
    InterestCategory category;
    String city;
    BigDecimal maxBudget;
    String query;
    boolean openOnly;
}

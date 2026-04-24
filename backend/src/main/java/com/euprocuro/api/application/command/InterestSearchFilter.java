package com.euprocuro.api.application.command;

import java.math.BigDecimal;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestSearchFilter {
    InterestCategory category;
    String city;
    BigDecimal maxBudget;
    String query;
    boolean openOnly;
}

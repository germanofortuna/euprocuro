package com.euprocuro.api.application.command;

import java.math.BigDecimal;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestSearchFilter {
    String category;
    String city;
    BigDecimal maxBudget;
    String query;
    boolean openOnly;
}

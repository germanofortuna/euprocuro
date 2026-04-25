package com.euprocuro.api.application.view;

import java.math.BigDecimal;

import com.euprocuro.api.domain.model.MonetizationProductType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonetizationProductView {
    String code;
    String name;
    String description;
    MonetizationProductType type;
    BigDecimal price;
    Integer credits;
    Integer durationDays;
}

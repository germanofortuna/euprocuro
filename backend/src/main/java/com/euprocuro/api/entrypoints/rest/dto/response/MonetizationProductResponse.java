package com.euprocuro.api.entrypoints.rest.dto.response;

import java.math.BigDecimal;

import com.euprocuro.api.domain.model.MonetizationProductType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonetizationProductResponse {
    String code;
    String name;
    String description;
    MonetizationProductType type;
    BigDecimal price;
    Integer credits;
    Integer durationDays;
}

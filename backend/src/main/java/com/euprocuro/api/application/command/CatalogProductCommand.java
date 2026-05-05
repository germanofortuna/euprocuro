package com.euprocuro.api.application.command;

import java.math.BigDecimal;

import com.euprocuro.api.domain.model.MonetizationProductType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CatalogProductCommand {
    String code;
    String name;
    String description;
    MonetizationProductType type;
    BigDecimal price;
    BigDecimal originalPrice;
    boolean promotional;
    String promotionLabel;
    Integer credits;
    Integer durationDays;
    boolean enabled;
    Integer sortOrder;
}

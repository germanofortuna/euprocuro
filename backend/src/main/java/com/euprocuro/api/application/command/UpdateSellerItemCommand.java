package com.euprocuro.api.application.command;

import java.math.BigDecimal;
import java.util.List;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UpdateSellerItemCommand {
    String title;
    String description;
    String referenceImageUrl;
    InterestCategory category;
    BigDecimal desiredPrice;
    String city;
    String state;
    String neighborhood;
    List<String> tags;
}

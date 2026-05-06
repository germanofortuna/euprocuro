package com.euprocuro.api.application.command;

import java.math.BigDecimal;
import java.util.List;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateSellerItemCommand {
    String title;
    String description;
    String referenceImageUrl;
    String category;
    BigDecimal desiredPrice;
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
    List<String> tags;
}

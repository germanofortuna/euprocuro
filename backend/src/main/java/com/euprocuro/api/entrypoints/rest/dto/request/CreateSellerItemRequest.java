package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import lombok.Data;

@Data
public class CreateSellerItemRequest {
    @NotBlank
    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(max = 120)
    private String description;

    @Size(max = 1500000)
    private String referenceImageUrl;

    @NotNull
    private String category;

    private BigDecimal desiredPrice;

    private String postalCode;
    private String city;
    private String state;
    private String neighborhood;
    private String country;
    private List<String> tags = new ArrayList<>();
}

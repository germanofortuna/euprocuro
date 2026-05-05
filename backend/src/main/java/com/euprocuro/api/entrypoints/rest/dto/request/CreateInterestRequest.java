package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


import lombok.Data;

@Data
public class CreateInterestRequest {
    @NotBlank
    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(max = 120)
    private String description;

    private String referenceImageUrl;

    @NotNull
    private String category;

    @DecimalMin("0.00")
    private BigDecimal budgetMin;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal budgetMax;

    private String postalCode;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    private String neighborhood;
    private String country;
    private Integer desiredRadiusKm;
    private boolean allowsWhatsappContact;
    private String whatsappContact;
    private String preferredCondition;
    private String preferredContactMode;
    private List<String> tags = new ArrayList<>();
}

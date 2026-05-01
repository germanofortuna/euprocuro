package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Data;

@Data
public class UpdateInterestRequest {
    @NotBlank
    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(max = 120)
    private String description;

    private String referenceImageUrl;

    @NotNull
    private InterestCategory category;

    @DecimalMin("0.00")
    private BigDecimal budgetMin;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal budgetMax;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    private String neighborhood;
    private Integer desiredRadiusKm;
    private boolean acceptsNationwideOffers;
    private boolean allowsWhatsappContact;
    private String whatsappContact;
    private boolean boostEnabled;
    private String preferredCondition;
    private String preferredContactMode;
    private List<String> tags = new ArrayList<>();
}

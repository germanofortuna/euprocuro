package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import lombok.Data;

@Data
public class UpdateInterestRequest {
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
    private StickerDetailsRequest stickerDetails;
    private boolean allowsWhatsappContact;
    private String whatsappContact;
    private String preferredCondition;
    private String preferredContactMode;
    private List<String> tags = new ArrayList<>();
}

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
public class CreateOfferRequest {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal offeredPrice;

    @Size(max = 40)
    @NotBlank
    private String sellerPhone;

    @Size(max = 1000)
    @NotBlank
    private String message;

    @Size(max = 1500000)
    private String offerImageUrl;

    private boolean includesDelivery;
    private List<String> highlights = new ArrayList<>();
}

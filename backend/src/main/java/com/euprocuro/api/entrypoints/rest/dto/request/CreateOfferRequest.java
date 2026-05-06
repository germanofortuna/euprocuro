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
public class CreateOfferRequest {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal offeredPrice;

    @NotBlank
    private String sellerPhone;

    @NotBlank
    @Size(max = 120)
    private String message;

    @Size(max = 1500000)
    private String offerImageUrl;

    private boolean includesDelivery;
    private List<String> highlights = new ArrayList<>();
}

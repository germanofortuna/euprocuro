package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ShareSellerItemRequest {
    @DecimalMin("0.00")
    private BigDecimal offeredPrice;

    @NotBlank
    private String sellerPhone;

    @Size(max = 120)
    private String message;

    private boolean includesDelivery;
}

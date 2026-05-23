package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class PurchaseProductRequest {
    @NotBlank
    private String productCode;
    private String paymentMethod;
}

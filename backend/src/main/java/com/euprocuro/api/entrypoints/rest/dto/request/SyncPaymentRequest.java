package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SyncPaymentRequest {
    @NotBlank
    private String paymentId;
}

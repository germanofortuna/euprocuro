package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class BoostInterestRequest {
    @NotBlank
    private String boostCode;
    private String paymentMethod;
}

package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ConfirmPhoneVerificationRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}

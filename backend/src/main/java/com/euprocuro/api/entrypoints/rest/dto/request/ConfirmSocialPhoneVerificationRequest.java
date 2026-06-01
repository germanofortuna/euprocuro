package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ConfirmSocialPhoneVerificationRequest {
    @NotBlank
    private String socialToken;

    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}

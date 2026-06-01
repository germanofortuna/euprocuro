package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class StartPhoneVerificationRequest {
    @NotBlank
    private String phone;

    private String turnstileToken;
}

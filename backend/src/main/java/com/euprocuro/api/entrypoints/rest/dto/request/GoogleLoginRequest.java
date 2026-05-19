package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank
    private String accessToken;
    private String turnstileToken;
}

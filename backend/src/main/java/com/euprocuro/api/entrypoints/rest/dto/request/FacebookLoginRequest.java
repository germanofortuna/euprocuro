package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class FacebookLoginRequest {
    @NotBlank
    private String accessToken;
    private String turnstileToken;
}

package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ConfirmRegistrationRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;

    private String turnstileToken;
}

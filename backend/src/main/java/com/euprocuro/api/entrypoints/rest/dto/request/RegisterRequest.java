package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String documentNumber;

    @NotBlank
    private String password;

    private String postalCode;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    private String neighborhood;
    private String country;

    @AssertTrue(message = "e necessario aceitar os termos de uso")
    private boolean termsAccepted;

    private String termsVersion;

    private String turnstileToken;

}

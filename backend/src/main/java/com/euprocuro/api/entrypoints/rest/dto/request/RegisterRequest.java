package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

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

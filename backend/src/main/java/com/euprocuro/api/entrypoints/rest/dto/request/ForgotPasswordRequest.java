package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @Email
    @NotBlank
    private String email;
}

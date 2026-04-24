package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}

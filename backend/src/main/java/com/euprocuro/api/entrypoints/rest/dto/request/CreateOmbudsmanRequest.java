package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateOmbudsmanRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank
    @Size(max = 120)
    private String type;

    @NotBlank
    @Size(max = 140)
    private String subject;

    @NotBlank
    @Size(max = 2000)
    private String message;

    @Size(max = 120)
    private String relatedEntityType;

    @Size(max = 120)
    private String relatedEntityId;

    @AssertTrue
    private boolean truthDeclarationAccepted;
}

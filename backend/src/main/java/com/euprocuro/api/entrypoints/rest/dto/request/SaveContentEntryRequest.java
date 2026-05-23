package com.euprocuro.api.entrypoints.rest.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.euprocuro.api.domain.model.ContentEntryType;

import lombok.Data;

@Data
public class SaveContentEntryRequest {
    @NotBlank
    @Size(max = 160)
    private String key;

    @NotNull
    private ContentEntryType type = ContentEntryType.TEXT;

    @Size(max = 12)
    private String locale = "pt-BR";

    @NotBlank
    @Size(max = 120000)
    private String draftValue;

    @Size(max = 500)
    private String description;

    @Size(max = 80)
    private String screen;

    @Size(max = 120)
    private String legalSlug;

    private boolean requiresUserAcceptance;
    private Instant effectiveFrom;
}

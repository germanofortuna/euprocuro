package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ReportInterestRequest {
    @NotBlank
    @Size(max = 80)
    private String reason;

    @Size(max = 120)
    private String message;
}

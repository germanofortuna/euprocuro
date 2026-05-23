package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.euprocuro.api.domain.model.InterestStatus;

import lombok.Data;

@Data
public class ModerationDecisionRequest {
    @NotNull
    private InterestStatus status;

    @Size(max = 120)
    private String reason;
}

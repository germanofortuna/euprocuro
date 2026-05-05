package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.euprocuro.api.domain.model.InterestStatus;

import lombok.Data;

@Data
public class ModerationDecisionRequest {
    @NotNull
    private InterestStatus status;

    @Size(max = 120)
    private String reason;
}

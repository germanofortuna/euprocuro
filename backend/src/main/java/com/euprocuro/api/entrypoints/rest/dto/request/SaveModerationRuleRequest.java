package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.Data;

@Data
public class SaveModerationRuleRequest {
    @NotBlank
    @Size(max = 80)
    private String term;

    @NotNull
    private ModerationRiskLevel riskLevel;

    private boolean active = true;
}

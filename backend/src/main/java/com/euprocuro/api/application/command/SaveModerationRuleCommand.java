package com.euprocuro.api.application.command;

import com.euprocuro.api.domain.model.ModerationRiskLevel;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveModerationRuleCommand {
    String term;
    ModerationRiskLevel riskLevel;
    boolean active;
}

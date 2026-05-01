package com.euprocuro.api.application.command;

import com.euprocuro.api.domain.model.InterestStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModerationDecisionCommand {
    InterestStatus status;
    String reason;
}

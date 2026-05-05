package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.ModerationContent;
import com.euprocuro.api.domain.model.ModerationResult;

public interface AiModerationGateway {
    Optional<ModerationResult> moderate(ModerationContent content);
}

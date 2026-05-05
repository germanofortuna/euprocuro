package com.euprocuro.api.application.usecase;

import com.euprocuro.api.application.command.ReportInterestCommand;
import com.euprocuro.api.domain.model.ContentReport;

public interface ModerationUseCase {
    void processInterestModeration(String interestId);

    ContentReport reportInterest(String currentUserId, String interestId, ReportInterestCommand command);
}

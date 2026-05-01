package com.euprocuro.api.application.usecase;

import com.euprocuro.api.application.command.ModerationDecisionCommand;
import com.euprocuro.api.application.command.SaveModerationRuleCommand;
import com.euprocuro.api.application.view.AdminModerationView;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.ModerationRule;

public interface AdminModerationUseCase {
    AdminModerationView getModerationQueue(String currentUserId);

    ModerationRule saveRule(String currentUserId, String ruleId, SaveModerationRuleCommand command);

    void deleteRule(String currentUserId, String ruleId);

    InterestPost decideInterest(String currentUserId, String interestId, ModerationDecisionCommand command);
}

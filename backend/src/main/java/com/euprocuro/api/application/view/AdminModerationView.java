package com.euprocuro.api.application.view;

import java.util.List;

import com.euprocuro.api.domain.model.InterestPost;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminModerationView {
    List<InterestPost> pendingInterests;
    List<ModerationRuleView> rules;
    List<ContentReportView> openReports;
}

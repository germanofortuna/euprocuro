package com.euprocuro.api.entrypoints.rest.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminModerationResponse {
    List<InterestResponse> pendingInterests;
    List<ModerationRuleResponse> rules;
    List<ContentReportResponse> openReports;
    List<ContentReportResponse> processedReports;
}

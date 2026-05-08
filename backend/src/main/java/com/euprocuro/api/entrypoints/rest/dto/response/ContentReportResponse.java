package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import com.euprocuro.api.domain.model.ContentReportStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentReportResponse {
    String id;
    String contentType;
    String contentId;
    String contentTitle;
    String contentDescription;
    String contentStatus;
    String reportedBy;
    String reason;
    String message;
    ContentReportStatus status;
    Instant createdAt;
    String reviewedBy;
    Instant reviewedAt;
}

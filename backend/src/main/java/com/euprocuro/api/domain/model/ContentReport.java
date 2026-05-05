package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContentReport {
    private String id;
    private String contentType;
    private String contentId;
    private String reportedBy;
    private String reason;
    private String message;
    private ContentReportStatus status;
    private Instant createdAt;
    private String reviewedBy;
    private Instant reviewedAt;
}

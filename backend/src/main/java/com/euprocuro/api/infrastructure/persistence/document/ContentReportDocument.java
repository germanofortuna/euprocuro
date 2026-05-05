package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.ContentReportStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("content_reports")
public class ContentReportDocument {
    @Id
    private String id;
    private String contentType;

    @Indexed
    private String contentId;

    private String reportedBy;
    private String reason;
    private String message;
    private ContentReportStatus status;
    private Instant createdAt;
    private String reviewedBy;
    private Instant reviewedAt;
}

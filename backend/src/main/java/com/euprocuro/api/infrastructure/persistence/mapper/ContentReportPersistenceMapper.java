package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.infrastructure.persistence.document.ContentReportDocument;

public final class ContentReportPersistenceMapper {

    private ContentReportPersistenceMapper() {
    }

    public static ContentReport toDomain(ContentReportDocument document) {
        if (document == null) {
            return null;
        }

        return ContentReport.builder()
                .id(document.getId())
                .contentType(document.getContentType())
                .contentId(document.getContentId())
                .reportedBy(document.getReportedBy())
                .reason(document.getReason())
                .message(document.getMessage())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .reviewedBy(document.getReviewedBy())
                .reviewedAt(document.getReviewedAt())
                .build();
    }

    public static ContentReportDocument toDocument(ContentReport domain) {
        if (domain == null) {
            return null;
        }

        return ContentReportDocument.builder()
                .id(domain.getId())
                .contentType(domain.getContentType())
                .contentId(domain.getContentId())
                .reportedBy(domain.getReportedBy())
                .reason(domain.getReason())
                .message(domain.getMessage())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .reviewedBy(domain.getReviewedBy())
                .reviewedAt(domain.getReviewedAt())
                .build();
    }
}

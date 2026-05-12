package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.domain.model.ContentReportStatus;

public interface ContentReportGateway {
    ContentReport save(ContentReport report);

    List<ContentReport> findByStatusOrderByCreatedAtDesc(ContentReportStatus status);

    List<ContentReport> findByStatusInOrderByCreatedAtDesc(List<ContentReportStatus> statuses);

    List<ContentReport> findByContentIdAndStatusOrderByCreatedAtDesc(String contentId, ContentReportStatus status);

    Optional<ContentReport> findById(String id);
}

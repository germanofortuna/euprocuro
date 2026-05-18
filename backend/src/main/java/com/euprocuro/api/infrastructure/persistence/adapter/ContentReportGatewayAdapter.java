package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.domain.model.ContentReportStatus;
import com.euprocuro.api.infrastructure.persistence.mapper.ContentReportPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataContentReportRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContentReportGatewayAdapter implements ContentReportGateway {

    private final SpringDataContentReportRepository repository;

    @Override
    public ContentReport save(ContentReport report) {
        return ContentReportPersistenceMapper.toDomain(repository.save(ContentReportPersistenceMapper.toDocument(report)));
    }

    @Override
    public List<ContentReport> findByStatusOrderByCreatedAtDesc(ContentReportStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(ContentReportPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentReport> findByStatusInOrderByCreatedAtDesc(List<ContentReportStatus> statuses) {
        return repository.findByStatusInOrderByCreatedAtDesc(statuses)
                .stream()
                .map(ContentReportPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentReport> findByContentIdAndStatusOrderByCreatedAtDesc(String contentId, ContentReportStatus status) {
        return repository.findByContentIdAndStatusOrderByCreatedAtDesc(contentId, status)
                .stream()
                .map(ContentReportPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ContentReport> findById(String id) {
        return repository.findById(id).map(ContentReportPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByReportedByOrContentIdIn(String reportedBy, List<String> contentIds) {
        repository.deleteByReportedByOrContentIdIn(reportedBy, contentIds == null ? List.of() : contentIds);
    }
}

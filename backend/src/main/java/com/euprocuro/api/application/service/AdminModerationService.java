package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.ModerationDecisionCommand;
import com.euprocuro.api.application.command.SaveModerationRuleCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.AdminModerationUseCase;
import com.euprocuro.api.application.view.AdminModerationView;
import com.euprocuro.api.application.view.ContentReportView;
import com.euprocuro.api.application.view.ModerationRuleView;
import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.ModerationRuleGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.domain.model.ContentReportStatus;
import com.euprocuro.api.domain.model.InterestModeration;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.domain.model.ModerationRule;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminModerationService implements AdminModerationUseCase {

    private static final Set<InterestStatus> MANUAL_DECISIONS = Set.of(
            InterestStatus.APPROVED,
            InterestStatus.REJECTED,
            InterestStatus.HIDDEN
    );

    private final AdminAccessService adminAccessService;
    private final InterestGateway interestGateway;
    private final ModerationRuleGateway moderationRuleGateway;
    private final ContentReportGateway contentReportGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final RealtimeMessageGateway realtimeMessageGateway;

    @Override
    public AdminModerationView getModerationQueue(String currentUserId) {
        adminAccessService.requireAdmin(currentUserId);
        List<InterestPost> pending = interestGateway.findAll()
                .stream()
                .filter(this::requiresReview)
                .collect(Collectors.toList());

        return AdminModerationView.builder()
                .pendingInterests(pending)
                .rules(moderationRuleGateway.findAll().stream().map(this::toView).collect(Collectors.toList()))
                .openReports(contentReportGateway.findByStatusOrderByCreatedAtDesc(ContentReportStatus.OPEN)
                        .stream()
                        .map(this::toView)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public ModerationRule saveRule(String currentUserId, String ruleId, SaveModerationRuleCommand command) {
        adminAccessService.requireAdmin(currentUserId);
        if (!StringUtils.hasText(command.getTerm())) {
            throw new BusinessException("Informe a palavra ou expressao da regra.");
        }

        Instant now = Instant.now();
        ModerationRule existingRule = StringUtils.hasText(ruleId)
                ? moderationRuleGateway.findById(ruleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Regra nao encontrada."))
                : null;
        ModerationRule rule = ModerationRule.builder()
                .id(existingRule == null ? null : existingRule.getId())
                .term(command.getTerm().trim().toLowerCase())
                .riskLevel(Optional.ofNullable(command.getRiskLevel()).orElse(ModerationRiskLevel.HIGH))
                .active(command.isActive())
                .createdAt(existingRule == null ? now : existingRule.getCreatedAt())
                .updatedAt(now)
                .build();
        return moderationRuleGateway.save(rule);
    }

    @Override
    public void deleteRule(String currentUserId, String ruleId) {
        adminAccessService.requireAdmin(currentUserId);
        moderationRuleGateway.deleteById(ruleId);
    }

    @Override
    public InterestPost decideInterest(String currentUserId, String interestId, ModerationDecisionCommand command) {
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        InterestStatus status = command.getStatus();
        if (!MANUAL_DECISIONS.contains(status)) {
            throw new BusinessException("Decisao de moderacao invalida.");
        }

        InterestPost interest = interestGateway.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        String reason = StringUtils.hasText(command.getReason())
                ? command.getReason().trim()
                : defaultReason(status);
        InterestPost saved = interestGateway.save(interest.toBuilder()
                .status(status)
                .updatedAt(Instant.now())
                .moderation(mergeModeration(interest.getModeration(), status, admin.getId(), reason))
                .build());
        eventPublisherGateway.publish("interest.moderation.decided", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId(),
                "status", saved.getStatus().name(),
                "adminId", admin.getId()
        ));
        realtimeMessageGateway.publishInterestModerationUpdated(saved.getOwnerId(), saved.getId(), saved.getStatus().name(), reason);
        return saved;
    }

    private boolean requiresReview(InterestPost interest) {
        return interest.getStatus() == InterestStatus.PENDING
                || interest.getStatus() == InterestStatus.REVIEW_REQUIRED
                || interest.getStatus() == InterestStatus.REPORTED;
    }

    private InterestModeration mergeModeration(
            InterestModeration current,
            InterestStatus status,
            String adminId,
            String reason
    ) {
        InterestModeration.InterestModerationBuilder builder = current == null
                ? InterestModeration.builder()
                : current.toBuilder();
        return builder
                .reviewRequired(status == InterestStatus.REVIEW_REQUIRED)
                .reason(reason)
                .reviewedBy(adminId)
                .reviewedAt(Instant.now())
                .build();
    }

    private String defaultReason(InterestStatus status) {
        if (status == InterestStatus.APPROVED) {
            return "Anuncio aprovado pela revisao manual.";
        }
        if (status == InterestStatus.HIDDEN) {
            return "Anuncio ocultado pela revisao manual.";
        }
        return "Anuncio rejeitado pela revisao manual.";
    }

    private ModerationRuleView toView(ModerationRule rule) {
        return ModerationRuleView.builder()
                .id(rule.getId())
                .term(rule.getTerm())
                .riskLevel(rule.getRiskLevel())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private ContentReportView toView(ContentReport report) {
        return ContentReportView.builder()
                .id(report.getId())
                .contentType(report.getContentType())
                .contentId(report.getContentId())
                .reportedBy(report.getReportedBy())
                .reason(report.getReason())
                .message(report.getMessage())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .build();
    }
}

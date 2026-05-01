package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.command.ModerationDecisionCommand;
import com.euprocuro.api.application.command.SaveModerationRuleCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.ModerationRuleGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.domain.model.ContentReportStatus;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.domain.model.ModerationRule;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private AdminAccessService adminAccessService;
    @Mock
    private InterestGateway interestGateway;
    @Mock
    private ModerationRuleGateway moderationRuleGateway;
    @Mock
    private ContentReportGateway contentReportGateway;
    @Mock
    private EventPublisherGateway eventPublisherGateway;
    @Mock
    private RealtimeMessageGateway realtimeMessageGateway;

    @InjectMocks
    private AdminModerationService adminModerationService;

    @Test
    void getModerationQueueShouldReturnReviewableInterestsRulesAndReports() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(interestGateway.findAll()).thenReturn(List.of(
                interest("pending", InterestStatus.PENDING),
                interest("review", InterestStatus.REVIEW_REQUIRED),
                interest("reported", InterestStatus.REPORTED),
                interest("approved", InterestStatus.APPROVED)
        ));
        when(moderationRuleGateway.findAll()).thenReturn(List.of(ModerationRule.builder()
                .id("rule-1")
                .term("bloqueado")
                .riskLevel(ModerationRiskLevel.HIGH)
                .active(true)
                .build()));
        when(contentReportGateway.findByStatusOrderByCreatedAtDesc(ContentReportStatus.OPEN))
                .thenReturn(List.of(ContentReport.builder()
                        .id("report-1")
                        .contentId("reported")
                        .reason("Suspeito")
                        .status(ContentReportStatus.OPEN)
                        .createdAt(Instant.now())
                        .build()));

        var result = adminModerationService.getModerationQueue("admin-1");

        assertThat(result.getPendingInterests()).extracting(InterestPost::getId)
                .containsExactly("pending", "review", "reported");
        assertThat(result.getRules()).hasSize(1);
        assertThat(result.getOpenReports()).hasSize(1);
    }

    @Test
    void saveRuleShouldCreateNormalizedRule() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(moderationRuleGateway.save(any(ModerationRule.class))).thenAnswer(invocation -> {
            ModerationRule rule = invocation.getArgument(0);
            rule.setId("rule-1");
            return rule;
        });

        ModerationRule result = adminModerationService.saveRule("admin-1", null, SaveModerationRuleCommand.builder()
                .term(" GOLPE ")
                .active(true)
                .build());

        assertThat(result.getId()).isEqualTo("rule-1");
        assertThat(result.getTerm()).isEqualTo("golpe");
        assertThat(result.getRiskLevel()).isEqualTo(ModerationRiskLevel.HIGH);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void saveRuleShouldUpdateExistingRule() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        ModerationRule existing = ModerationRule.builder()
                .id("rule-1")
                .term("antigo")
                .riskLevel(ModerationRiskLevel.LOW)
                .active(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(moderationRuleGateway.findById("rule-1")).thenReturn(Optional.of(existing));
        when(moderationRuleGateway.save(any(ModerationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModerationRule result = adminModerationService.saveRule("admin-1", "rule-1", SaveModerationRuleCommand.builder()
                .term("Novo")
                .riskLevel(ModerationRiskLevel.MEDIUM)
                .active(false)
                .build());

        assertThat(result.getId()).isEqualTo("rule-1");
        assertThat(result.getTerm()).isEqualTo("novo");
        assertThat(result.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void saveRuleShouldRejectBlankTerm() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());

        assertThatThrownBy(() -> adminModerationService.saveRule("admin-1", null, SaveModerationRuleCommand.builder()
                .term(" ")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("palavra");
    }

    @Test
    void deleteRuleShouldDelegateToGateway() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());

        adminModerationService.deleteRule("admin-1", "rule-1");

        verify(moderationRuleGateway).deleteById("rule-1");
    }

    @Test
    void decideInterestShouldSaveManualDecisionAndPublishEvents() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest("interest-1", InterestStatus.REVIEW_REQUIRED)));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterestPost result = adminModerationService.decideInterest("admin-1", "interest-1", ModerationDecisionCommand.builder()
                .status(InterestStatus.APPROVED)
                .reason("Tudo certo")
                .build());

        assertThat(result.getStatus()).isEqualTo(InterestStatus.APPROVED);
        assertThat(result.getModeration().getReviewedBy()).isEqualTo("admin-1");
        verify(eventPublisherGateway).publish(eq("interest.moderation.decided"), any(Map.class));
        verify(realtimeMessageGateway).publishInterestModerationUpdated(
                eq("buyer-1"),
                eq("interest-1"),
                eq("APPROVED"),
                eq("Tudo certo")
        );
    }

    @Test
    void decideInterestShouldRejectInvalidStatus() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());

        assertThatThrownBy(() -> adminModerationService.decideInterest("admin-1", "interest-1", ModerationDecisionCommand.builder()
                .status(InterestStatus.PENDING)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalida");
    }

    @Test
    void decideInterestShouldUseDefaultReasonForHiddenStatus() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest("interest-1", InterestStatus.REPORTED)));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminModerationService.decideInterest("admin-1", "interest-1", ModerationDecisionCommand.builder()
                .status(InterestStatus.HIDDEN)
                .build());

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        assertThat(captor.getValue().getModeration().getReason()).contains("ocultado");
    }

    private UserProfile admin() {
        return UserProfile.builder()
                .id("admin-1")
                .name("Admin")
                .email("admin@teste.com")
                .build();
    }

    private InterestPost interest(String id, InterestStatus status) {
        return InterestPost.builder()
                .id(id)
                .ownerId("buyer-1")
                .ownerName("Ana")
                .title("Procuro item")
                .description("Descricao curta")
                .category(InterestCategory.OUTROS)
                .budgetMax(new BigDecimal("100"))
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

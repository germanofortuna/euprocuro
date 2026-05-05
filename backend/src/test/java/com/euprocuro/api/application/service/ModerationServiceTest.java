package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.ReportInterestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.domain.gateway.AiModerationGateway;
import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.ModerationRuleGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.domain.model.ContentReportStatus;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.ModerationContent;
import com.euprocuro.api.domain.model.ModerationResult;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.domain.model.ModerationRule;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private InterestGateway interestGateway;
    @Mock
    private ModerationRuleGateway moderationRuleGateway;
    @Mock
    private ContentReportGateway contentReportGateway;
    @Mock
    private AiModerationGateway aiModerationGateway;
    @Mock
    private RealtimeMessageGateway realtimeMessageGateway;

    @InjectMocks
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(moderationService, "defaultBlockedTerms", "");
        ReflectionTestUtils.setField(moderationService, "reviewThreshold", 0.55);
        ReflectionTestUtils.setField(moderationService, "rejectThreshold", 0.85);
    }

    @Test
    void processInterestModerationShouldRejectLinksBeforeAiModeration() {
        InterestPost interest = pendingInterest().toBuilder()
                .description("Conheca os detalhes em https://golpe.example")
                .build();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.processInterestModeration("interest-1");

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        InterestPost saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InterestStatus.REJECTED);
        assertThat(saved.getModeration().getRiskLevel()).isEqualTo(ModerationRiskLevel.HIGH);
        assertThat(saved.getModeration().getReason()).contains("Links");
        verify(aiModerationGateway, never()).moderate(any(ModerationContent.class));
        verify(realtimeMessageGateway).publishInterestModerationUpdated(
                eq("buyer-1"),
                eq("interest-1"),
                eq("REJECTED"),
                any(String.class)
        );
    }

    @Test
    void processInterestModerationShouldApproveWhenNoRuleOrAiFlag() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest()));
        when(moderationRuleGateway.findByActiveTrue()).thenReturn(List.of());
        when(aiModerationGateway.moderate(any(ModerationContent.class))).thenReturn(Optional.empty());
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.processInterestModeration("interest-1");

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        InterestPost saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InterestStatus.APPROVED);
        assertThat(saved.getModeration().getProvider()).isEqualTo("LOCAL_ONLY");
        assertThat(saved.getModeration().isReviewRequired()).isFalse();
    }

    @Test
    void processInterestModerationShouldRequestReviewForMediumLocalRuleWithoutAi() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest()));
        when(moderationRuleGateway.findByActiveTrue()).thenReturn(List.of(ModerationRule.builder()
                .term("duvidoso")
                .riskLevel(ModerationRiskLevel.MEDIUM)
                .active(true)
                .build()));
        when(aiModerationGateway.moderate(any(ModerationContent.class))).thenReturn(Optional.empty());
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.processInterestModeration("interest-1");

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterestStatus.REVIEW_REQUIRED);
        assertThat(captor.getValue().getModeration().isReviewRequired()).isTrue();
    }

    @Test
    void processInterestModerationShouldMatchLocalRulesIgnoringCase() {
        InterestPost interest = pendingInterest().toBuilder()
                .title("Procuro PRODUTO BLOQUEADO")
                .description("Quero comprar em bom estado")
                .build();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(moderationRuleGateway.findByActiveTrue()).thenReturn(List.of(ModerationRule.builder()
                .term("produto bloqueado")
                .riskLevel(ModerationRiskLevel.HIGH)
                .active(true)
                .build()));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.processInterestModeration("interest-1");

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterestStatus.REJECTED);
        verify(aiModerationGateway, never()).moderate(any(ModerationContent.class));
    }

    @Test
    void processInterestModerationShouldRejectFlaggedAiResult() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest()));
        when(moderationRuleGateway.findByActiveTrue()).thenReturn(List.of());
        when(aiModerationGateway.moderate(any(ModerationContent.class))).thenReturn(Optional.of(ModerationResult.builder()
                .flagged(true)
                .provider("OPENAI")
                .categories(Map.of("illicit",true))
                .scores(Map.of("illicit", 0.3))
                .build()));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.processInterestModeration("interest-1");

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterestStatus.REJECTED);
        assertThat(captor.getValue().getModeration().getProvider()).isEqualTo("OPENAI");
    }

    @Test
    void processInterestModerationShouldRequestReviewWhenAiScoreReachesThreshold() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest()));
        when(moderationRuleGateway.findByActiveTrue()).thenReturn(List.of());
        when(aiModerationGateway.moderate(any(ModerationContent.class))).thenReturn(Optional.of(ModerationResult.builder()
                .flagged(false)
                .provider("OPENAI")
                .categories(Map.of("suspicious", true))
                .scores(Map.of("suspicious", 0.7))
                .build()));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.processInterestModeration("interest-1");

        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterestStatus.REVIEW_REQUIRED);
        assertThat(captor.getValue().getModeration().getCategories())
                .containsEntry("suspicious", true);

        assertThat(captor.getValue().getModeration().getScores())
                .containsEntry("suspicious", 0.7);

        assertThat(captor.getValue().getModeration().isReviewRequired()).isTrue();
        assertThat(captor.getValue().getModeration().getProvider()).isEqualTo("OPENAI");
    }

    @Test
    void processInterestModerationShouldIgnoreNonPendingInterest() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest().toBuilder()
                .status(InterestStatus.APPROVED)
                .build()));

        moderationService.processInterestModeration("interest-1");

        verify(interestGateway, never()).save(any(InterestPost.class));
        verify(moderationRuleGateway, never()).findByActiveTrue();
    }

    @Test
    void reportInterestShouldCreateReportAndMarkInterestAsReported() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest().toBuilder()
                .status(InterestStatus.APPROVED)
                .build()));
        when(contentReportGateway.save(any(ContentReport.class))).thenAnswer(invocation -> {
            ContentReport report = invocation.getArgument(0);
            report.setId("report-1");
            return report;
        });
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContentReport result = moderationService.reportInterest("seller-1", "interest-1", ReportInterestCommand.builder()
                .reason("Conteudo suspeito")
                .message("Parece golpe")
                .build());

        assertThat(result.getId()).isEqualTo("report-1");
        assertThat(result.getStatus()).isEqualTo(ContentReportStatus.OPEN);
        ArgumentCaptor<InterestPost> captor = ArgumentCaptor.forClass(InterestPost.class);
        verify(interestGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterestStatus.REPORTED);
        assertThat(captor.getValue().getModeration().getProvider()).isEqualTo("USER_REPORT");
    }

    @Test
    void reportInterestShouldRejectPrivateInterest() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pendingInterest()));

        assertThatThrownBy(() -> moderationService.reportInterest("seller-1", "interest-1", ReportInterestCommand.builder()
                .reason("Suspeito")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("denuncia");
    }

    private InterestPost pendingInterest() {
        return InterestPost.builder()
                .id("interest-1")
                .ownerId("buyer-1")
                .ownerName("Ana")
                .title("Procuro produto duvidoso")
                .description("Procuro item usado em bom estado")
                .category("OUTROS")
                .budgetMin(BigDecimal.ZERO)
                .budgetMax(new BigDecimal("500"))
                .location(LocationInfo.builder()
                        .city("Erechim")
                        .state("RS")
                        .build())
                .tags(List.of("usado"))
                .status(InterestStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.command.CreateOmbudsmanRequestCommand;
import com.euprocuro.api.application.command.RespondOmbudsmanRequestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.OmbudsmanRequestGateway;
import com.euprocuro.api.domain.model.OmbudsmanRequest;
import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class OmbudsmanServiceTest {

    @Mock
    private OmbudsmanRequestGateway ombudsmanRequestGateway;
    @Mock
    private AdminAccessService adminAccessService;
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private AuditLogService auditLogService;

    private OmbudsmanService service;

    @BeforeEach
    void setUp() {
        service = new OmbudsmanService(ombudsmanRequestGateway, adminAccessService, emailGateway, auditLogService);
    }

    @Test
    void createShouldSaveOpenRequestSendConfirmationAndAudit() {
        when(ombudsmanRequestGateway.save(any(OmbudsmanRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OmbudsmanRequest.class).toBuilder()
                        .id("omb-1")
                        .build());

        var view = service.create(validCreateCommand().build());

        ArgumentCaptor<OmbudsmanRequest> captor = ArgumentCaptor.forClass(OmbudsmanRequest.class);
        verify(ombudsmanRequestGateway).save(captor.capture());

        OmbudsmanRequest saved = captor.getValue();
        assertThat(saved.getProtocol()).startsWith("OUV-");
        assertThat(saved.getName()).isEqualTo("Germano");
        assertThat(saved.getEmail()).isEqualTo("germano@teste.com");
        assertThat(saved.getStatus()).isEqualTo(OmbudsmanRequestStatus.OPEN);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(view.getId()).isEqualTo("omb-1");
        assertThat(view.getProtocol()).isEqualTo(saved.getProtocol());
        verify(emailGateway).sendOmbudsmanConfirmationEmail("Germano", "germano@teste.com", saved.getProtocol(), "Assunto");
        verify(auditLogService).record("OMBUDSMAN_REQUEST_CREATED", null, "germano@teste.com", "OMBUDSMAN", "omb-1");
    }

    @Test
    void createShouldRejectInvalidPayloads() {
        assertThatThrownBy(() -> service.create(validCreateCommand()
                .truthDeclarationAccepted(false)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("veracidade");

        assertThatThrownBy(() -> service.create(validCreateCommand()
                .email("email-invalido")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail valido");

        assertThatThrownBy(() -> service.create(validCreateCommand()
                .message(" ")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mensagem");
    }

    @Test
    void listAdminShouldRequireAdminAndFilterByStatus() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(ombudsmanRequestGateway.findByStatusOrderByCreatedAtDesc(OmbudsmanRequestStatus.OPEN))
                .thenReturn(List.of(existingRequest().status(OmbudsmanRequestStatus.OPEN).build()));

        var views = service.listAdmin("admin-1", OmbudsmanRequestStatus.OPEN);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getStatus()).isEqualTo(OmbudsmanRequestStatus.OPEN);
        verify(adminAccessService).requireAdmin("admin-1");
        verify(ombudsmanRequestGateway).findByStatusOrderByCreatedAtDesc(OmbudsmanRequestStatus.OPEN);
    }

    @Test
    void listAdminWithoutStatusShouldReturnAllOrderedRequests() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(ombudsmanRequestGateway.findAllOrderByCreatedAtDesc())
                .thenReturn(List.of(existingRequest().build()));

        var views = service.listAdmin("admin-1", null);

        assertThat(views).extracting("protocol").containsExactly("OUV-2026-ABC12345");
        verify(ombudsmanRequestGateway).findAllOrderByCreatedAtDesc();
    }

    @Test
    void respondShouldSaveAnswerSendEmailAndAudit() {
        OmbudsmanRequest existing = existingRequest().build();
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(ombudsmanRequestGateway.findById("omb-1")).thenReturn(Optional.of(existing));
        when(ombudsmanRequestGateway.save(any(OmbudsmanRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OmbudsmanRequest.class));

        var view = service.respond("admin-1", "omb-1", RespondOmbudsmanRequestCommand.builder()
                .adminResponse("Resposta oficial")
                .status(OmbudsmanRequestStatus.ANSWERED)
                .build());

        ArgumentCaptor<OmbudsmanRequest> captor = ArgumentCaptor.forClass(OmbudsmanRequest.class);
        verify(ombudsmanRequestGateway).save(captor.capture());

        OmbudsmanRequest saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OmbudsmanRequestStatus.ANSWERED);
        assertThat(saved.getAdminResponse()).isEqualTo("Resposta oficial");
        assertThat(saved.getAnsweredBy()).isEqualTo("admin-1");
        assertThat(saved.getAnsweredAt()).isNotNull();
        assertThat(saved.getClosedAt()).isNull();
        assertThat(view.getAdminResponse()).isEqualTo("Resposta oficial");

        verify(emailGateway).sendOmbudsmanResponseEmail(
                "Germano",
                "germano@teste.com",
                "OUV-2026-ABC12345",
                "Assunto",
                "Resposta oficial"
        );
        verify(auditLogService).record(
                eq("OMBUDSMAN_REQUEST_RESPONDED"),
                eq("admin-1"),
                eq("admin@teste.com"),
                eq("OMBUDSMAN"),
                eq("omb-1"),
                eq(AuditLogService.OUTCOME_SUCCESS),
                any()
        );
    }

    @Test
    void respondShouldRejectMissingRequestInvalidStatusAndBlankResponse() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());

        assertThatThrownBy(() -> service.respond("admin-1", "missing", RespondOmbudsmanRequestCommand.builder()
                .adminResponse("Resposta")
                .status(OmbudsmanRequestStatus.ANSWERED)
                .build()))
                .isInstanceOf(ResourceNotFoundException.class);

        when(ombudsmanRequestGateway.findById("omb-1")).thenReturn(Optional.of(existingRequest().build()));

        assertThatThrownBy(() -> service.respond("admin-1", "omb-1", RespondOmbudsmanRequestCommand.builder()
                .adminResponse(" ")
                .status(OmbudsmanRequestStatus.ANSWERED)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("resposta");

        assertThatThrownBy(() -> service.respond("admin-1", "omb-1", RespondOmbudsmanRequestCommand.builder()
                .adminResponse("Resposta")
                .status(OmbudsmanRequestStatus.OPEN)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Status invalido");
    }

    @Test
    void updateStatusShouldCloseRequestWhenStatusIsClosed() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin());
        when(ombudsmanRequestGateway.findById("omb-1")).thenReturn(Optional.of(existingRequest().build()));
        when(ombudsmanRequestGateway.save(any(OmbudsmanRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OmbudsmanRequest.class));

        var view = service.updateStatus("admin-1", "omb-1", OmbudsmanRequestStatus.CLOSED);

        assertThat(view.getStatus()).isEqualTo(OmbudsmanRequestStatus.CLOSED);
        assertThat(view.getClosedAt()).isNotNull();
        verify(auditLogService).record("OMBUDSMAN_REQUEST_STATUS_UPDATED", "admin-1", "admin@teste.com", "OMBUDSMAN", "omb-1");
    }

    private CreateOmbudsmanRequestCommand.CreateOmbudsmanRequestCommandBuilder validCreateCommand() {
        return CreateOmbudsmanRequestCommand.builder()
                .name("Germano")
                .email("GERMANO@TESTE.COM")
                .type("Reclamacao")
                .subject("Assunto")
                .message("Mensagem da ouvidoria")
                .relatedEntityType("INTEREST")
                .relatedEntityId("interest-1")
                .truthDeclarationAccepted(true);
    }

    private OmbudsmanRequest.OmbudsmanRequestBuilder existingRequest() {
        return OmbudsmanRequest.builder()
                .id("omb-1")
                .protocol("OUV-2026-ABC12345")
                .name("Germano")
                .email("germano@teste.com")
                .type("Reclamacao")
                .subject("Assunto")
                .message("Mensagem")
                .status(OmbudsmanRequestStatus.OPEN)
                .createdAt(Instant.parse("2026-05-07T10:00:00Z"))
                .updatedAt(Instant.parse("2026-05-07T10:00:00Z"));
    }

    private UserProfile admin() {
        return UserProfile.builder()
                .id("admin-1")
                .email("admin@teste.com")
                .name("Admin")
                .build();
    }
}

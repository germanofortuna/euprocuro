package com.euprocuro.api.application.service;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.CreateOmbudsmanRequestCommand;
import com.euprocuro.api.application.command.RespondOmbudsmanRequestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.view.OmbudsmanRequestView;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.OmbudsmanRequestGateway;
import com.euprocuro.api.domain.model.OmbudsmanRequest;
import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OmbudsmanService {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_SUBJECT_LENGTH = 140;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_REFERENCE_LENGTH = 120;

    private final OmbudsmanRequestGateway ombudsmanRequestGateway;
    private final AdminAccessService adminAccessService;
    private final EmailGateway emailGateway;
    private final AuditLogService auditLogService;

    public OmbudsmanRequestView create(CreateOmbudsmanRequestCommand command) {
        validateCreate(command);
        Instant now = Instant.now();
        OmbudsmanRequest saved = ombudsmanRequestGateway.save(OmbudsmanRequest.builder()
                .protocol(generateProtocol())
                .name(clean(command.getName(), MAX_NAME_LENGTH))
                .email(normalizeEmail(command.getEmail()))
                .type(clean(command.getType(), MAX_REFERENCE_LENGTH))
                .subject(clean(command.getSubject(), MAX_SUBJECT_LENGTH))
                .message(clean(command.getMessage(), MAX_MESSAGE_LENGTH))
                .relatedEntityType(cleanOptional(command.getRelatedEntityType(), MAX_REFERENCE_LENGTH))
                .relatedEntityId(cleanOptional(command.getRelatedEntityId(), MAX_REFERENCE_LENGTH))
                .status(OmbudsmanRequestStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build());

        emailGateway.sendOmbudsmanConfirmationEmail(
                saved.getName(),
                saved.getEmail(),
                saved.getProtocol(),
                saved.getSubject()
        );
        auditLogService.record("OMBUDSMAN_REQUEST_CREATED", null, saved.getEmail(), "OMBUDSMAN", saved.getId());
        return toView(saved);
    }

    public List<OmbudsmanRequestView> listAdmin(String currentUserId, OmbudsmanRequestStatus status) {
        adminAccessService.requireAdmin(currentUserId);
        List<OmbudsmanRequest> requests = status == null
                ? ombudsmanRequestGateway.findAllOrderByCreatedAtDesc()
                : ombudsmanRequestGateway.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toView).collect(Collectors.toList());
    }

    public OmbudsmanRequestView respond(String currentUserId, String requestId, RespondOmbudsmanRequestCommand command) {
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        OmbudsmanRequest request = ombudsmanRequestGateway.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Manifestacao de ouvidoria nao encontrada."));

        if (!StringUtils.hasText(command.getAdminResponse())) {
            throw new BusinessException("Informe a resposta da Ouvidoria.");
        }

        Instant now = Instant.now();
        OmbudsmanRequestStatus nextStatus = Optional.ofNullable(command.getStatus())
                .orElse(OmbudsmanRequestStatus.ANSWERED);
        if (nextStatus != OmbudsmanRequestStatus.ANSWERED && nextStatus != OmbudsmanRequestStatus.CLOSED) {
            throw new BusinessException("Status invalido para resposta da Ouvidoria.");
        }

        OmbudsmanRequest saved = ombudsmanRequestGateway.save(request.toBuilder()
                .status(nextStatus)
                .adminResponse(clean(command.getAdminResponse(), MAX_MESSAGE_LENGTH))
                .answeredBy(admin.getId())
                .answeredAt(now)
                .closedAt(nextStatus == OmbudsmanRequestStatus.CLOSED ? now : request.getClosedAt())
                .updatedAt(now)
                .build());

        emailGateway.sendOmbudsmanResponseEmail(
                saved.getName(),
                saved.getEmail(),
                saved.getProtocol(),
                saved.getSubject(),
                saved.getAdminResponse()
        );
        auditLogService.record(
                "OMBUDSMAN_REQUEST_RESPONDED",
                admin.getId(),
                admin.getEmail(),
                "OMBUDSMAN",
                saved.getId(),
                AuditLogService.OUTCOME_SUCCESS,
                Map.of("protocol", saved.getProtocol(), "status", saved.getStatus().name())
        );
        return toView(saved);
    }

    public OmbudsmanRequestView updateStatus(String currentUserId, String requestId, OmbudsmanRequestStatus status) {
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        OmbudsmanRequest request = ombudsmanRequestGateway.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Manifestacao de ouvidoria nao encontrada."));
        OmbudsmanRequestStatus nextStatus = Optional.ofNullable(status).orElse(OmbudsmanRequestStatus.IN_REVIEW);
        Instant now = Instant.now();
        OmbudsmanRequest saved = ombudsmanRequestGateway.save(request.toBuilder()
                .status(nextStatus)
                .closedAt(nextStatus == OmbudsmanRequestStatus.CLOSED ? now : request.getClosedAt())
                .updatedAt(now)
                .build());
        auditLogService.record("OMBUDSMAN_REQUEST_STATUS_UPDATED", admin.getId(), admin.getEmail(), "OMBUDSMAN", saved.getId());
        return toView(saved);
    }

    private void validateCreate(CreateOmbudsmanRequestCommand command) {
        if (!command.isTruthDeclarationAccepted()) {
            throw new BusinessException("Confirme a declaracao de veracidade para enviar a manifestacao.");
        }
        if (!StringUtils.hasText(command.getName())) {
            throw new BusinessException("Informe seu nome.");
        }
        if (!StringUtils.hasText(command.getEmail()) || !normalizeEmail(command.getEmail()).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("Informe um e-mail valido.");
        }
        if (!StringUtils.hasText(command.getType())) {
            throw new BusinessException("Informe o tipo de manifestacao.");
        }
        if (!StringUtils.hasText(command.getSubject())) {
            throw new BusinessException("Informe o assunto.");
        }
        if (!StringUtils.hasText(command.getMessage())) {
            throw new BusinessException("Informe a mensagem.");
        }
    }

    private String generateProtocol() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "OUV-" + Year.now().getValue() + "-" + suffix;
    }

    private String normalizeEmail(String value) {
        return clean(value, MAX_REFERENCE_LENGTH).toLowerCase(Locale.ROOT);
    }

    private String clean(String value, int maxLength) {
        String cleaned = Optional.ofNullable(value).orElse("").trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    private String cleanOptional(String value, int maxLength) {
        String cleaned = clean(value, maxLength);
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    private OmbudsmanRequestView toView(OmbudsmanRequest request) {
        return OmbudsmanRequestView.builder()
                .id(request.getId())
                .protocol(request.getProtocol())
                .userId(request.getUserId())
                .name(request.getName())
                .email(request.getEmail())
                .type(request.getType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .status(request.getStatus())
                .adminResponse(request.getAdminResponse())
                .answeredBy(request.getAnsweredBy())
                .answeredAt(request.getAnsweredAt())
                .closedAt(request.getClosedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}

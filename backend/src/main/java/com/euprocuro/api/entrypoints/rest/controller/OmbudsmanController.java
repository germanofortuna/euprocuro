package com.euprocuro.api.entrypoints.rest.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.command.CreateOmbudsmanRequestCommand;
import com.euprocuro.api.application.command.RespondOmbudsmanRequestCommand;
import com.euprocuro.api.application.service.OmbudsmanService;
import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateOmbudsmanRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.RespondOmbudsmanRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateOmbudsmanStatusRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.OmbudsmanRequestResponse;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Ouvidoria", description = "Endpoints para envio e tratamento de manifestacoes da Ouvidoria.")
public class OmbudsmanController {

    private final OmbudsmanService ombudsmanService;

    @PostMapping("/ouvidoria")
    @ResponseStatus(HttpStatus.CREATED)
    public OmbudsmanRequestResponse create(@Valid @RequestBody CreateOmbudsmanRequest request) {
        return toResponse(ombudsmanService.create(CreateOmbudsmanRequestCommand.builder()
                .name(request.getName())
                .email(request.getEmail())
                .type(request.getType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .truthDeclarationAccepted(request.isTruthDeclarationAccepted())
                .build()));
    }

    @GetMapping("/admin/ouvidoria")
    public List<OmbudsmanRequestResponse> listAdmin(
            HttpServletRequest request,
            @RequestParam(required = false) OmbudsmanRequestStatus status
    ) {
        return ombudsmanService.listAdmin(CurrentUserContext.userId(request), status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/admin/ouvidoria/{id}/response")
    public OmbudsmanRequestResponse respond(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody RespondOmbudsmanRequest requestBody
    ) {
        return toResponse(ombudsmanService.respond(
                CurrentUserContext.userId(request),
                id,
                RespondOmbudsmanRequestCommand.builder()
                        .adminResponse(requestBody.getAdminResponse())
                        .status(requestBody.getStatus())
                        .build()
        ));
    }

    @PatchMapping("/admin/ouvidoria/{id}/status")
    public OmbudsmanRequestResponse updateStatus(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody UpdateOmbudsmanStatusRequest requestBody
    ) {
        return toResponse(ombudsmanService.updateStatus(
                CurrentUserContext.userId(request),
                id,
                requestBody.getStatus()
        ));
    }

    private OmbudsmanRequestResponse toResponse(com.euprocuro.api.application.view.OmbudsmanRequestView view) {
        return OmbudsmanRequestResponse.builder()
                .id(view.getId())
                .protocol(view.getProtocol())
                .userId(view.getUserId())
                .name(view.getName())
                .email(view.getEmail())
                .type(view.getType())
                .subject(view.getSubject())
                .message(view.getMessage())
                .relatedEntityType(view.getRelatedEntityType())
                .relatedEntityId(view.getRelatedEntityId())
                .status(view.getStatus())
                .adminResponse(view.getAdminResponse())
                .answeredBy(view.getAnsweredBy())
                .answeredAt(view.getAnsweredAt())
                .closedAt(view.getClosedAt())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }
}

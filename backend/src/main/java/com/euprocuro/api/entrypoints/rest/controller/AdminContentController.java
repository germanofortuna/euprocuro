package com.euprocuro.api.entrypoints.rest.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.usecase.AdminContentUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveContentEntryRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.AdminContentCatalogResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.ContentEntryResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.ContentRevisionResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
@Tag(name = "Admin Content", description = "CRM/CMS interno para rascunho, publicacao e versionamento de textos da plataforma.")
public class AdminContentController {

    private final AdminContentUseCase adminContentUseCase;

    @GetMapping
    public AdminContentCatalogResponse getEntries(HttpServletRequest request) {
        return RestMapper.toResponse(adminContentUseCase.getContentEntries(CurrentUserContext.userId(request)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentEntryResponse createEntry(
            HttpServletRequest request,
            @Valid @RequestBody SaveContentEntryRequest requestBody
    ) {
        return RestMapper.toResponse(adminContentUseCase.saveDraft(
                CurrentUserContext.userId(request),
                null,
                RestMapper.toCommand(requestBody)
        ));
    }

    @PutMapping("/{id}")
    public ContentEntryResponse updateEntry(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody SaveContentEntryRequest requestBody
    ) {
        return RestMapper.toResponse(adminContentUseCase.saveDraft(
                CurrentUserContext.userId(request),
                id,
                RestMapper.toCommand(requestBody)
        ));
    }

    @PostMapping("/{id}/publish")
    public ContentEntryResponse publishEntry(@PathVariable String id, HttpServletRequest request) {
        return RestMapper.toResponse(adminContentUseCase.publish(CurrentUserContext.userId(request), id));
    }

    @PostMapping("/{id}/archive")
    public ContentEntryResponse archiveEntry(@PathVariable String id, HttpServletRequest request) {
        return RestMapper.toResponse(adminContentUseCase.archive(CurrentUserContext.userId(request), id));
    }

    @PostMapping("/{id}/apply-default")
    public ContentEntryResponse applyDefaultDraft(@PathVariable String id, HttpServletRequest request) {
        return RestMapper.toResponse(adminContentUseCase.applyDefaultDraft(CurrentUserContext.userId(request), id));
    }

    @PostMapping("/{id}/dismiss-default")
    public ContentEntryResponse dismissDefaultUpdate(@PathVariable String id, HttpServletRequest request) {
        return RestMapper.toResponse(adminContentUseCase.dismissDefaultUpdate(CurrentUserContext.userId(request), id));
    }

    @GetMapping("/{id}/revisions")
    public List<ContentRevisionResponse> getRevisions(@PathVariable String id, HttpServletRequest request) {
        return adminContentUseCase.getRevisions(CurrentUserContext.userId(request), id)
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }
}

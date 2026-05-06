package com.euprocuro.api.entrypoints.rest.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.service.OperationalCatalogService;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveOperationalCatalogRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.AdminOperationalCatalogResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@Tag(name = "Admin Catalog", description = "CRM operacional para categorias, precos e promocoes publicados em runtime.")
public class AdminOperationalCatalogController {

    private final OperationalCatalogService operationalCatalogService;

    @GetMapping
    public AdminOperationalCatalogResponse getCatalog(HttpServletRequest request) {
        return RestMapper.toResponse(operationalCatalogService.getAdminCatalog(CurrentUserContext.userId(request)));
    }

    @PutMapping
    public AdminOperationalCatalogResponse saveCatalog(
            HttpServletRequest request,
            @Valid @RequestBody SaveOperationalCatalogRequest requestBody
    ) {
        return RestMapper.toResponse(operationalCatalogService.saveAdminCatalog(
                CurrentUserContext.userId(request),
                RestMapper.toCommand(requestBody)
        ));
    }
}

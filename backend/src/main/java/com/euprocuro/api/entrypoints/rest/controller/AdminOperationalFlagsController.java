package com.euprocuro.api.entrypoints.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.service.OperationalCatalogService;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveOperationalFlagsRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.AdminOperationalCatalogResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/operational-flags")
@RequiredArgsConstructor
@Tag(name = "Admin Operational Flags", description = "Flags operacionais independentes de produtos e categorias.")
public class AdminOperationalFlagsController {

    private final OperationalCatalogService operationalCatalogService;

    @PutMapping
    public AdminOperationalCatalogResponse saveFlags(
            HttpServletRequest request,
            @Valid @RequestBody SaveOperationalFlagsRequest requestBody
    ) {
        return RestMapper.toResponse(operationalCatalogService.saveOperationalFlags(
                CurrentUserContext.userId(request),
                RestMapper.toCommand(requestBody)
        ));
    }
}

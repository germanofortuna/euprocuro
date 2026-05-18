package com.euprocuro.api.entrypoints.rest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.service.OperationalCatalogService;
import com.euprocuro.api.entrypoints.rest.dto.response.PublicOperationalSettingsResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operational")
@RequiredArgsConstructor
@Tag(name = "Operational Settings", description = "Flags publicas e campos operacionais do runtime.")
public class OperationalSettingsController {

    private final OperationalCatalogService operationalCatalogService;

    @GetMapping("/public")
    public PublicOperationalSettingsResponse publicSettings() {
        return RestMapper.toResponse(operationalCatalogService.getPublicOperationalSettings());
    }
}

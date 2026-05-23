package com.euprocuro.api.entrypoints.rest.controller;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.service.AdminCacheService;
import com.euprocuro.api.entrypoints.rest.dto.response.CacheInvalidationResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Tag(name = "Admin Cache", description = "Invalidacao manual dos caches publicos da plataforma.")
public class AdminCacheController {

    private final AdminCacheService adminCacheService;

    @GetMapping
    public CacheInvalidationResponse status(HttpServletRequest request) {
        return RestMapper.toResponse(adminCacheService.getStatus(CurrentUserContext.userId(request)));
    }

    @PostMapping("/invalidate")
    public CacheInvalidationResponse invalidate(
            HttpServletRequest request,
            @RequestParam(defaultValue = "all") String scope
    ) {
        return RestMapper.toResponse(adminCacheService.invalidate(CurrentUserContext.userId(request), scope));
    }
}

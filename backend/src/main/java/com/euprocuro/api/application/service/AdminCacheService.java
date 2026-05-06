package com.euprocuro.api.application.service;

import org.springframework.stereotype.Service;

import com.euprocuro.api.application.view.CacheInvalidationView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCacheService {

    private final AdminAccessService adminAccessService;
    private final PublicCacheService publicCacheService;
    private final AuditLogService auditLogService;

    public CacheInvalidationView getStatus(String currentUserId) {
        adminAccessService.requireAdmin(currentUserId);
        return publicCacheService.snapshot();
    }

    public CacheInvalidationView invalidate(String currentUserId, String scope) {
        var admin = adminAccessService.requireAdmin(currentUserId);
        CacheInvalidationView view = publicCacheService.invalidate(scope);
        auditLogService.record("CACHE_INVALIDATED", admin.getId(), admin.getEmail(), "CACHE", scope);
        return view;
    }
}

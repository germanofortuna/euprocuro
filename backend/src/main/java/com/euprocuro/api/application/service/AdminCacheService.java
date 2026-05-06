package com.euprocuro.api.application.service;

import org.springframework.stereotype.Service;

import com.euprocuro.api.application.view.CacheInvalidationView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCacheService {

    private final AdminAccessService adminAccessService;
    private final PublicCacheService publicCacheService;

    public CacheInvalidationView getStatus(String currentUserId) {
        adminAccessService.requireAdmin(currentUserId);
        return publicCacheService.snapshot();
    }

    public CacheInvalidationView invalidate(String currentUserId, String scope) {
        adminAccessService.requireAdmin(currentUserId);
        return publicCacheService.invalidate(scope);
    }
}

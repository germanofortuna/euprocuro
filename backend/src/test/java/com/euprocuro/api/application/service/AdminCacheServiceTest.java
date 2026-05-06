package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.euprocuro.api.application.view.CacheInvalidationView;
import com.euprocuro.api.domain.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCacheServiceTest {

    @Mock
    private AdminAccessService adminAccessService;
    @Mock
    private PublicCacheService publicCacheService;

    @Test
    void getStatusAndInvalidateShouldRequireAdminAndDelegateToPublicCache() {
        AdminCacheService service = new AdminCacheService(adminAccessService, publicCacheService);
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(UserProfile.builder().id("admin-1").build());
        when(publicCacheService.snapshot()).thenReturn(CacheInvalidationView.builder()
                .scope(PublicCacheService.ALL)
                .versions(Map.of())
                .build());
        when(publicCacheService.invalidate(PublicCacheService.CONTENT)).thenReturn(CacheInvalidationView.builder()
                .scope(PublicCacheService.CONTENT)
                .versions(Map.of(PublicCacheService.CONTENT, 2L))
                .build());

        assertThat(service.getStatus("admin-1").getScope()).isEqualTo(PublicCacheService.ALL);
        assertThat(service.invalidate("admin-1", PublicCacheService.CONTENT).getVersions())
                .containsEntry(PublicCacheService.CONTENT, 2L);

        verify(adminAccessService, times(2)).requireAdmin("admin-1");
        verify(publicCacheService).snapshot();
        verify(publicCacheService).invalidate(PublicCacheService.CONTENT);
    }
}

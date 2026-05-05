package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class AdminAccessServiceTest {

    @Mock
    private UserGateway userGateway;

    @InjectMocks
    private AdminAccessService adminAccessService;

    @Test
    void requireAdminShouldAllowConfiguredEmailIgnoringCase() {
        ReflectionTestUtils.setField(adminAccessService, "allowedAdminEmails", " admin@teste.com ");
        UserProfile admin = UserProfile.builder()
                .id("admin-1")
                .email("ADMIN@teste.com")
                .build();
        when(userGateway.findById("admin-1")).thenReturn(Optional.of(admin));

        UserProfile result = adminAccessService.requireAdmin("admin-1");

        assertThat(result).isSameAs(admin);
    }

    @Test
    void requireAdminShouldRejectUserOutsideAllowedList() {
        ReflectionTestUtils.setField(adminAccessService, "allowedAdminEmails", "admin@teste.com");
        when(userGateway.findById("user-1")).thenReturn(Optional.of(UserProfile.builder()
                .id("user-1")
                .email("user@teste.com")
                .build()));

        assertThatThrownBy(() -> adminAccessService.requireAdmin("user-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("administrativo");
    }

    @Test
    void requireAdminShouldRejectMissingUser() {
        when(userGateway.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAccessService.requireAdmin("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario nao encontrado");
    }
}

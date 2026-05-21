package com.euprocuro.api.application.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final UserGateway userGateway;

    @Value("${application.admin.allowed-emails:}")
    private String allowedAdminEmails;

    public boolean isAdmin(String email) {
        return adminEmails().contains(safeEmail(email));
    }

    public UserProfile requireAdmin(String userId) {
        UserProfile user = userGateway.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
        if (!isAdmin(user.getEmail())) {
            throw new ForbiddenException("Acesso administrativo nao autorizado.");
        }
        return user;
    }

    private Set<String> adminEmails() {
        return Arrays.stream(allowedAdminEmails.split(","))
                .map(this::safeEmail)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private String safeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

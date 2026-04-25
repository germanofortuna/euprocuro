package com.euprocuro.api.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.RegisterUserCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.domain.gateway.AuthSessionGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.PasswordResetTokenGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserGateway userGateway;
    private final AuthSessionGateway authSessionGateway;
    private final PasswordResetTokenGateway passwordResetTokenGateway;
    private final PasswordEncoder passwordEncoder;
    private final EmailGateway emailGateway;
    private final EventPublisherGateway eventPublisherGateway;

    @Value("${application.auth.session-hours:168}")
    private long sessionHours;

    @Value("${application.auth.password-reset-hours:2}")
    private long passwordResetHours;

    @Value("${application.auth.reset-base-url:http://localhost:5173}")
    private String resetBaseUrl;

    @Value("${application.auth.expose-reset-preview:true}")
    private boolean exposeResetPreview;

    @Override
    public AuthenticatedSessionView register(RegisterUserCommand command) {
        validatePassword(command.getPassword());

        userGateway.findByEmail(command.getEmail()).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este e-mail.");
        });

        UserProfile user = userGateway.save(UserProfile.builder()
                .name(command.getName())
                .email(command.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .city(command.getCity())
                .state(command.getState())
                .bio(Optional.ofNullable(command.getBio()).orElse(""))
                .buyerRating(4.8)
                .sellerRating(4.8)
                .sellerCredits(3)
                .purchasedCreditsTotal(0)
                .build());

        eventPublisherGateway.publish("user.registered", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));

        AuthSession session = createSession(user);
        return AuthenticatedSessionView.builder()
                .token(session.getToken())
                .expiresAt(session.getExpiresAt())
                .user(user)
                .build();
    }

    @Override
    public AuthenticatedSessionView login(LoginCommand command) {
        UserProfile user = userGateway.findByEmail(command.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha invalidos."));

        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("E-mail ou senha invalidos.");
        }

        AuthSession session = createSession(user);
        eventPublisherGateway.publish("auth.login", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "expiresAt", session.getExpiresAt()
        ));

        return AuthenticatedSessionView.builder()
                .token(session.getToken())
                .expiresAt(session.getExpiresAt())
                .user(user)
                .build();
    }

    @Override
    public AuthenticatedSessionView me(String token) {
        UserProfile user = requireAuthenticatedUser(token);
        AuthSession session = getValidSession(token);
        return AuthenticatedSessionView.builder()
                .token(session.getToken())
                .expiresAt(session.getExpiresAt())
                .user(user)
                .build();
    }

    @Override
    public void logout(String token) {
        UserProfile user = requireAuthenticatedUser(token);
        authSessionGateway.deleteByToken(token);
        eventPublisherGateway.publish("auth.logout", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    @Override
    public PasswordResetRequestView forgotPassword(ForgotPasswordCommand command) {
        Optional<UserProfile> optionalUser = userGateway.findByEmail(command.getEmail().trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            return PasswordResetRequestView.builder()
                    .message("Se o e-mail existir, enviaremos as instrucoes de redefinicao.")
                    .build();
        }

        UserProfile user = optionalUser.get();
        PasswordResetToken resetToken = passwordResetTokenGateway.save(PasswordResetToken.builder()
                .token(UUID.randomUUID().toString().replace("-", ""))
                .userId(user.getId())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(passwordResetHours, ChronoUnit.HOURS))
                .build());

        String resetLink = buildResetLink(resetToken.getToken());
        boolean sent = emailGateway.sendPasswordResetEmail(user, resetLink);

        eventPublisherGateway.publish("auth.password-reset-requested", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "sentByEmail", sent
        ));

        return PasswordResetRequestView.builder()
                .message("Se o e-mail existir, enviaremos as instrucoes de redefinicao.")
                .previewResetLink(sent || !exposeResetPreview ? null : resetLink)
                .previewToken(sent || !exposeResetPreview ? null : resetToken.getToken())
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        if (!StringUtils.hasText(command.getToken())) {
            throw new BusinessException("Token de redefinicao invalido.");
        }

        validatePassword(command.getNewPassword());
        if (!command.getNewPassword().equals(command.getConfirmPassword())) {
            throw new BusinessException("A confirmacao da senha nao confere.");
        }

        PasswordResetToken resetToken = passwordResetTokenGateway.findByToken(command.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Token de redefinicao nao encontrado."));

        if (resetToken.getUsedAt() != null) {
            throw new BusinessException("Este token ja foi utilizado.");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Este token de redefinicao expirou.");
        }

        UserProfile user = userGateway.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        userGateway.save(user.toBuilder()
                .passwordHash(passwordEncoder.encode(command.getNewPassword()))
                .build());

        passwordResetTokenGateway.save(resetToken.toBuilder()
                .usedAt(Instant.now())
                .build());

        eventPublisherGateway.publish("auth.password-reset-completed", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    @Override
    public UserProfile requireAuthenticatedUser(String token) {
        AuthSession session = getValidSession(token);
        return userGateway.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Sessao invalida."));
    }

    private AuthSession createSession(UserProfile user) {
        return authSessionGateway.save(AuthSession.builder()
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .userId(user.getId())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(sessionHours, ChronoUnit.HOURS))
                .build());
    }

    private AuthSession getValidSession(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("Sessao nao informada.");
        }

        AuthSession session = authSessionGateway.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Sessao invalida."));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            authSessionGateway.deleteByToken(token);
            throw new UnauthorizedException("Sessao expirada.");
        }

        return session;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.trim().length() < 6) {
            throw new BusinessException("A senha deve ter pelo menos 6 caracteres.");
        }
    }

    private String buildResetLink(String token) {
        String normalizedBase = resetBaseUrl.endsWith("/") ? resetBaseUrl.substring(0, resetBaseUrl.length() - 1) : resetBaseUrl;
        return normalizedBase + "?mode=reset&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}

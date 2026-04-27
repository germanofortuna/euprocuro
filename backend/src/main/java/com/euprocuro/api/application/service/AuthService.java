package com.euprocuro.api.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "10minutemail.com",
            "guerrillamail.com",
            "mailinator.com",
            "tempmail.com",
            "temp-mail.org",
            "yopmail.com"
    );

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
        String normalizedName = normalizeName(command.getName());
        String normalizedEmail = normalizeEmail(command.getEmail());
        String normalizedDocument = normalizeDocument(command.getDocumentNumber());
        String documentType = documentType(normalizedDocument);

        validateName(normalizedName);
        validateEmail(normalizedEmail);
        validateDocument(normalizedDocument);
        validatePassword(command.getPassword());

        userGateway.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este e-mail.");
        });
        userGateway.findByDocumentNumber(normalizedDocument).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este CPF/CNPJ.");
        });

        UserProfile user = userGateway.save(UserProfile.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .documentNumber(normalizedDocument)
                .documentType(documentType)
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .city(normalizeText(command.getCity()))
                .state(normalizeState(command.getState()))
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

    private void validateName(String name) {
        if (!StringUtils.hasText(name) || name.length() < 5 || name.split("\\s+").length < 2) {
            throw new BusinessException("Informe nome e sobrenome para criar a conta.");
        }
    }

    private void validateEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            throw new BusinessException("Informe um e-mail valido.");
        }

        String domain = email.substring(atIndex + 1);
        if (DISPOSABLE_EMAIL_DOMAINS.contains(domain)) {
            throw new BusinessException("Use um e-mail permanente para criar a conta.");
        }
    }

    private void validateDocument(String documentNumber) {
        boolean valid = documentNumber.length() == 11
                ? isValidCpf(documentNumber)
                : documentNumber.length() == 14 && isValidCnpj(documentNumber);

        if (!valid) {
            throw new BusinessException("Informe um CPF ou CNPJ valido.");
        }
    }

    private void validatePassword(String password) {
        String value = Optional.ofNullable(password).orElse("");
        if (value.length() < 8 || !value.matches(".*[A-Za-z].*") || !value.matches(".*\\d.*")) {
            throw new BusinessException("A senha deve ter pelo menos 8 caracteres, com letras e numeros.");
        }
    }

    private String normalizeName(String value) {
        return normalizeText(value).replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String value) {
        return normalizeText(value).toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }

    private String normalizeState(String value) {
        String state = normalizeText(value).toUpperCase(Locale.ROOT);
        if (state.length() > 2) {
            throw new BusinessException("Informe a UF com 2 letras.");
        }
        return state;
    }

    private String normalizeDocument(String value) {
        return Optional.ofNullable(value).orElse("").replaceAll("\\D", "");
    }

    private String documentType(String documentNumber) {
        return documentNumber.length() == 11 ? "CPF" : "CNPJ";
    }

    private boolean isValidCpf(String documentNumber) {
        if (hasAllSameDigits(documentNumber)) {
            return false;
        }

        int firstDigit = calculateCpfDigit(documentNumber, 9);
        int secondDigit = calculateCpfDigit(documentNumber, 10);
        return firstDigit == Character.getNumericValue(documentNumber.charAt(9))
                && secondDigit == Character.getNumericValue(documentNumber.charAt(10));
    }

    private int calculateCpfDigit(String documentNumber, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(documentNumber.charAt(index)) * (length + 1 - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private boolean isValidCnpj(String documentNumber) {
        if (hasAllSameDigits(documentNumber)) {
            return false;
        }

        int firstDigit = calculateCnpjDigit(documentNumber, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int secondDigit = calculateCnpjDigit(documentNumber, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return firstDigit == Character.getNumericValue(documentNumber.charAt(12))
                && secondDigit == Character.getNumericValue(documentNumber.charAt(13));
    }

    private int calculateCnpjDigit(String documentNumber, int[] weights) {
        int sum = 0;
        for (int index = 0; index < weights.length; index++) {
            sum += Character.getNumericValue(documentNumber.charAt(index)) * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private boolean hasAllSameDigits(String documentNumber) {
        return documentNumber.chars().distinct().count() == 1;
    }

    private String buildResetLink(String token) {
        String normalizedBase = resetBaseUrl.endsWith("/") ? resetBaseUrl.substring(0, resetBaseUrl.length() - 1) : resetBaseUrl;
        return normalizedBase + "?mode=reset&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}

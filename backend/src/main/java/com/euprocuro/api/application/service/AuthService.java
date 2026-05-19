package com.euprocuro.api.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.GoogleLoginCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.RegisterUserCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.GoogleIdentityView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.domain.gateway.AuthSessionGateway;
import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.gateway.ConversationMessageGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EmailVerificationTokenGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.PasswordResetTokenGateway;
import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.domain.model.EmailVerificationToken;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.infrastructure.security.GoogleIdentityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private static final String DELETED_USER_LABEL = "Usuário excluído";
    private static final String REMOVED_OFFER_MESSAGE = "[Proposta removida por exclusão de conta]";
    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "10minutemail.com",
            "guerrillamail.com",
            "mailinator.com",
            "tempmail.com",
            "temp-mail.org",
            "yopmail.com"
    );
    private static final String CURRENT_TERMS_VERSION = "2026-05-05";

    private final UserGateway userGateway;
    private final AuthSessionGateway authSessionGateway;
    private final PasswordResetTokenGateway passwordResetTokenGateway;
    private final EmailVerificationTokenGateway emailVerificationTokenGateway;
    private final InterestGateway interestGateway;
    private final OfferGateway offerGateway;
    private final ConversationMessageGateway conversationMessageGateway;
    private final SellerItemGateway sellerItemGateway;
    private final ContentReportGateway contentReportGateway;
    private final PasswordEncoder passwordEncoder;
    private final EmailGateway emailGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final AuditLogService auditLogService;
    private final OperationalCatalogService operationalCatalogService;
    private final GoogleIdentityService googleIdentityService;

    @Value("${application.auth.session-hours:168}")
    private long sessionHours;

    @Value("${application.auth.session-renewal-threshold-hours:24}")
    private long sessionRenewalThresholdHours;

    @Value("${application.auth.password-reset-hours:2}")
    private long passwordResetHours;

    @Value("${application.auth.email-verification-hours:24}")
    private long emailVerificationHours;

    @Value("${application.auth.reset-base-url:http://localhost:5173}")
    private String resetBaseUrl;

    @Value("${application.auth.expose-reset-preview:false}")
    private boolean exposeResetPreview;

    @Value("${application.hml.access.enabled:false}")
    private boolean hmlAccessEnabled;

    @Value("${application.hml.access.allowed-emails:}")
    private String hmlAllowedEmails;

    @Value("${application.auth.email-verification-required:true}")
    private boolean emailVerificationRequired;

    @Override
    public RegistrationView register(RegisterUserCommand command) {
        String normalizedName = normalizeName(command.getName());
        String normalizedEmail = normalizeEmail(command.getEmail());
        String normalizedDocument = normalizeDocument(command.getDocumentNumber());
        String documentType = documentType(normalizedDocument);

        validateName(normalizedName);
        validateEmail(normalizedEmail);
        validateHmlAccess(normalizedEmail);
        validateDocument(normalizedDocument);
        validatePassword(command.getPassword());

        userGateway.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este e-mail.");
        });
        userGateway.findByDocumentNumber(normalizedDocument).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este CPF/CNPJ.");
        });

        if (!command.isTermsAccepted()) {
            throw new BusinessException("E necessario aceitar os termos de uso para criar a conta.");
        }

        UserProfile user = userGateway.save(UserProfile.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .documentNumber(normalizedDocument)
                .documentType(documentType)
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .postalCode(normalizePostalCode(command.getPostalCode()))
                .city(normalizeText(command.getCity()))
                .state(normalizeState(command.getState()))
                .neighborhood(normalizeText(command.getNeighborhood()))
                .country(normalizeCountry(command.getCountry()))
                .emailVerified(!emailVerificationRequired)
                .buyerRating(4.8)
                .sellerRating(4.8)
                .sellerCredits(operationalCatalogService.initialFreeCredits())
                .purchasedCreditsTotal(0)
                .ipAddress(command.getIpAddress())
                .termsAccepted(true)
                .termsAcceptedAt(Instant.now())
                .termsVersion(StringUtils.hasText(command.getTermsVersion())
                        ? command.getTermsVersion()
                        : CURRENT_TERMS_VERSION)
                .build());

        boolean verificationSent = emailVerificationRequired && sendRequiredEmailVerification(user);
        auditLogService.record("USER_REGISTERED", user.getId(), user.getEmail(), "USER", user.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("emailVerificationRequired", emailVerificationRequired));

        eventPublisherGateway.publish("user.registered", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "verificationSentByEmail", verificationSent
        ));

        String message = emailVerificationRequired
                ? "Conta criada. Enviamos um link para confirmar seu e-mail antes do login."
                : "Conta criada";

        return RegistrationView.builder()
                .verificationSentByEmail(verificationSent)
                .message(message)
                .build();
    }

    private boolean sendRequiredEmailVerification(UserProfile user) {
        EmailVerificationToken verificationToken = null;

        try {
            verificationToken = emailVerificationTokenGateway.save(buildEmailVerificationToken(user));
            String verificationLink = buildEmailVerificationLink(verificationToken.getToken());
            boolean verificationSent = emailGateway.sendEmailVerificationEmail(user, verificationLink);

            if (verificationSent) {
                return true;
            }
        } catch (RuntimeException exception) {
            rollbackPendingRegistration(user, verificationToken);
            throw new BusinessException("Não foi possível enviar o e-mail de confirmação. Tente novamente mais tarde.");
        }

        rollbackPendingRegistration(user, verificationToken);
        throw new BusinessException("Não foi possível enviar o e-mail de confirmação. Tente novamente mais tarde.");
    }

    private EmailVerificationToken buildEmailVerificationToken(UserProfile user) {
        Instant now = Instant.now();
        return EmailVerificationToken.builder()
                .token(UUID.randomUUID().toString().replace("-", ""))
                .userId(user.getId())
                .createdAt(now)
                .expiresAt(now.plus(emailVerificationHours, ChronoUnit.HOURS))
                .build();
    }

    private void rollbackPendingRegistration(UserProfile user, EmailVerificationToken verificationToken) {
        if (verificationToken != null && StringUtils.hasText(verificationToken.getToken())) {
            emailVerificationTokenGateway.deleteByToken(verificationToken.getToken());
        }

        if (user != null && StringUtils.hasText(user.getId())) {
            userGateway.deleteById(user.getId());
        }
    }

    @Override
    public AuthenticatedSessionView login(LoginCommand command) {
        String normalizedEmail = normalizeEmail(command.getEmail());
        validateHmlAccess(normalizedEmail);

        UserProfile user = userGateway.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha invalidos."));

        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("E-mail ou senha invalidos.");
        }

        if (emailVerificationRequired && !user.isEmailVerified()) {
            throw new BusinessException("Confirme seu e-mail antes de entrar.");
        }

        AuthSession session = createSession(user);
        auditLogService.record("AUTH_LOGIN", user.getId(), user.getEmail(), "AUTH_SESSION", session.getToken(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("expiresAt", session.getExpiresAt()));
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
    public AuthenticatedSessionView loginWithGoogle(GoogleLoginCommand command) {
        GoogleIdentityView identity = googleIdentityService.verify(command.getIdToken());
        String normalizedEmail = normalizeEmail(identity.getEmail());
        validateHmlAccess(normalizedEmail);

        UserProfile user = userGateway.findByEmail(normalizedEmail)
                .map(existing -> existing.isEmailVerified()
                        ? existing
                        : userGateway.save(existing.toBuilder()
                                .emailVerified(true)
                                .build()))
                .orElseGet(() -> userGateway.save(UserProfile.builder()
                        .name(googleDisplayName(identity))
                        .email(normalizedEmail)
                        .emailVerified(true)
                        .buyerRating(4.8)
                        .sellerRating(4.8)
                        .sellerCredits(operationalCatalogService.initialFreeCredits())
                        .purchasedCreditsTotal(0)
                        .ipAddress(command.getIpAddress())
                        .termsAccepted(true)
                        .termsAcceptedAt(Instant.now())
                        .termsVersion(CURRENT_TERMS_VERSION)
                        .country("Brasil")
                        .build()));

        AuthSession session = createSession(user);
        auditLogService.record("AUTH_GOOGLE_LOGIN", user.getId(), user.getEmail(), "AUTH_SESSION", session.getToken(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("expiresAt", session.getExpiresAt()));
        eventPublisherGateway.publish("auth.google-login", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "createdByGoogle", !StringUtils.hasText(user.getPasswordHash()),
                "expiresAt", session.getExpiresAt()
        ));

        return AuthenticatedSessionView.builder()
                .token(session.getToken())
                .expiresAt(session.getExpiresAt())
                .user(user)
                .build();
    }

    private String googleDisplayName(GoogleIdentityView identity) {
        String name = normalizeName(identity.getName());
        if (StringUtils.hasText(name)) {
            return name;
        }
        String email = normalizeEmail(identity.getEmail());
        int atIndex = email.indexOf("@");
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : "Usuario";
        return localPart.replace(".", " ").replace("_", " ").trim();
    }

    @Override
    public UserProfile meByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }

        return userGateway.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao encontrado."));
    }

    @Override
    public void logout(String token) {
        UserProfile user = requireAuthenticatedUser(token);
        authSessionGateway.deleteByToken(token);
        auditLogService.record("AUTH_LOGOUT", user.getId(), user.getEmail(), "AUTH_SESSION", token);
        eventPublisherGateway.publish("auth.logout", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    @Override
    public void logoutIfPresent(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        authSessionGateway.findByToken(token)
                .ifPresent(session -> {
                    authSessionGateway.deleteByToken(token);

                    userGateway.findById(session.getUserId())
                            .ifPresent(user -> eventPublisherGateway.publish(
                                    "auth.logout",
                                    Map.of("userId", user.getId(),"email", user.getEmail())
                            ));
                });
    }

    @Override
    public void deleteCurrentUser(String userId) {
        UserProfile user = userGateway.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao encontrado."));

        List<String> ownedInterestIds = interestGateway.findByOwnerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InterestPost::getId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        List<Offer> offersOnOwnedInterests = ownedInterestIds.isEmpty()
                ? List.of()
                : offerGateway.findByInterestPostIdInOrderByCreatedAtDesc(ownedInterestIds);
        List<String> ownedOfferIds = offersOnOwnedInterests.stream()
                .map(Offer::getId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        List<Offer> anonymizedSentOffers = offerGateway.findBySellerIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(offer -> !ownedInterestIds.contains(offer.getInterestPostId()))
                .map(this::anonymizeDeletedUserOffer)
                .collect(Collectors.toList());
        anonymizedSentOffers.forEach(offerGateway::save);

        conversationMessageGateway.deleteByOfferIdIn(ownedOfferIds);
        offerGateway.deleteByIdIn(ownedOfferIds);
        conversationMessageGateway.anonymizeByUserId(userId);
        contentReportGateway.deleteByReportedByOrContentIdIn(userId, ownedInterestIds);
        sellerItemGateway.deleteByOwnerId(userId);
        interestGateway.deleteByOwnerId(userId);
        passwordResetTokenGateway.deleteByUserId(userId);
        emailVerificationTokenGateway.deleteByUserId(userId);
        authSessionGateway.deleteByUserId(userId);
        userGateway.deleteById(userId);

        auditLogService.record("USER_ACCOUNT_DELETED", user.getId(), null, "USER", user.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of(
                        "deletedInterests", ownedInterestIds.size(),
                        "deletedOffers", ownedOfferIds.size(),
                        "anonymizedOffers", anonymizedSentOffers.size()
                ));
        eventPublisherGateway.publish("user.account-deleted", Map.of(
                "userId", user.getId()
        ));
    }

    private Offer anonymizeDeletedUserOffer(Offer offer) {
        return offer.toBuilder()
                .sellerId(null)
                .sellerName(DELETED_USER_LABEL)
                .sellerEmail(null)
                .sellerPhone(null)
                .message(REMOVED_OFFER_MESSAGE)
                .offerImageUrl(null)
                .highlights(List.of())
                .build();
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
    public void verifyEmail(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("Token de verificacao invalido.");
        }

        EmailVerificationToken verificationToken = emailVerificationTokenGateway.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacao nao encontrado."));

        if (verificationToken.getUsedAt() != null) {
            UserProfile user = userGateway.findById(verificationToken.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
            if (user.isEmailVerified()) {
                return;
            }
            throw new BusinessException("Este e-mail ja foi verificado.");
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Este token de verificacao expirou.");
        }

        UserProfile user = userGateway.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        userGateway.save(user.toBuilder()
                .emailVerified(true)
                .build());
        auditLogService.record("AUTH_EMAIL_VERIFIED", user.getId(), user.getEmail(), "USER", user.getId());

        emailVerificationTokenGateway.save(verificationToken.toBuilder()
                .usedAt(Instant.now())
                .build());

        eventPublisherGateway.publish("auth.email-verified", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    @Override
    public UserProfile requireAuthenticatedUser(String token) {
        return requireAuthenticatedSession(token).getUser();
    }

    @Override
    public AuthenticatedSessionView requireAuthenticatedSession(String token) {
        AuthSession session = getValidSession(token);
        AuthSession validSession = renewSessionIfNeeded(session);
        UserProfile user = userGateway.findById(validSession.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida."));

        return AuthenticatedSessionView.builder()
                .token(validSession.getToken())
                .expiresAt(validSession.getExpiresAt())
                .user(user)
                .renewed(!Objects.equals(session.getExpiresAt(), validSession.getExpiresAt()))
                .build();
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

    private AuthSession renewSessionIfNeeded(AuthSession session) {
        if (sessionRenewalThresholdHours <= 0) {
            return session;
        }

        Instant now = Instant.now();
        Instant renewalBoundary = now.plus(sessionRenewalThresholdHours, ChronoUnit.HOURS);
        if (session.getExpiresAt().isAfter(renewalBoundary)) {
            return session;
        }

        return authSessionGateway.save(session.toBuilder()
                .expiresAt(now.plus(sessionHours, ChronoUnit.HOURS))
                .build());
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

    private void validateHmlAccess(String email) {
        if (!hmlAccessEnabled) {
            return;
        }

        boolean allowed = Arrays.stream(Optional.ofNullable(hmlAllowedEmails).orElse("").split(","))
                .map(this::normalizeEmail)
                .filter(StringUtils::hasText)
                .anyMatch(allowedEmail -> allowedEmail.equals(email));

        if (!allowed) {
            throw new BusinessException("Ambiente restrito! Entre em contato com um administrador.");
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

    private String normalizePostalCode(String value) {
        String digits = Optional.ofNullable(value).orElse("").replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        if (digits.length() != 8) {
            throw new BusinessException("Informe um CEP valido com 8 digitos.");
        }
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

    private String normalizeCountry(String value) {
        String country = normalizeText(value);
        return StringUtils.hasText(country) ? country : "Brasil";
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

    private String buildEmailVerificationLink(String token) {
        String normalizedBase = resetBaseUrl.endsWith("/") ? resetBaseUrl.substring(0, resetBaseUrl.length() - 1) : resetBaseUrl;
        return normalizedBase + "?mode=verify-email&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}

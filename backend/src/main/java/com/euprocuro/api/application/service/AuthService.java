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

import com.euprocuro.api.application.command.ConfirmPhoneVerificationCommand;
import com.euprocuro.api.application.command.ConfirmRegistrationCommand;
import com.euprocuro.api.application.command.ConfirmSocialPhoneVerificationCommand;
import com.euprocuro.api.application.command.FacebookLoginCommand;
import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.GoogleLoginCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.command.StartPhoneVerificationCommand;
import com.euprocuro.api.application.command.StartRegistrationCommand;
import com.euprocuro.api.application.command.StartSocialPhoneVerificationCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.FacebookIdentityView;
import com.euprocuro.api.application.view.GoogleIdentityView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.application.view.SocialAuthView;
import com.euprocuro.api.domain.gateway.AuthSessionGateway;
import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.gateway.ConversationMessageGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EmailVerificationTokenGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.PasswordResetTokenGateway;
import com.euprocuro.api.domain.gateway.PendingRegistrationGateway;
import com.euprocuro.api.domain.gateway.PendingSocialAuthGateway;
import com.euprocuro.api.domain.gateway.PhoneVerificationGateway;
import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.domain.model.PendingRegistration;
import com.euprocuro.api.domain.model.PendingSocialAuth;
import com.euprocuro.api.domain.model.PhoneVerificationChannel;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.infrastructure.security.FacebookIdentityService;
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
    private final PendingRegistrationGateway pendingRegistrationGateway;
    private final PendingSocialAuthGateway pendingSocialAuthGateway;
    private final PhoneVerificationGateway phoneVerificationGateway;
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
    private final FacebookIdentityService facebookIdentityService;

    @Value("${application.auth.session-hours:168}")
    private long sessionHours;

    @Value("${application.auth.session-renewal-threshold-hours:24}")
    private long sessionRenewalThresholdHours;

    @Value("${application.auth.password-reset-hours:2}")
    private long passwordResetHours;

    @Value("${application.auth.pending-registration-minutes:15}")
    private long pendingRegistrationMinutes;

    @Value("${application.auth.phone-verification.channel:SMS}")
    private String phoneVerificationChannel;

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
    public RegistrationView startRegistration(StartRegistrationCommand command) {
        String normalizedName = normalizeName(command.getName());
        String normalizedEmail = normalizeEmail(command.getEmail());
        String normalizedPhone = normalizePhone(command.getPhone());

        validateName(normalizedName);
        validateEmail(normalizedEmail);
        validateHmlAccess(normalizedEmail);
        validatePassword(command.getPassword());

        if (!command.isTermsAccepted()) {
            throw new BusinessException("E necessario aceitar os termos de uso para criar a conta.");
        }

        userGateway.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este e-mail.");
        });
        userGateway.findByPhone(normalizedPhone).ifPresent(existing -> {
            throw new BusinessException("Ja existe usuario com este telefone.");
        });

        Instant now = Instant.now();
        pendingRegistrationGateway.deleteByEmail(normalizedEmail);
        pendingRegistrationGateway.save(PendingRegistration.builder()
                .email(normalizedEmail)
                .phone(normalizedPhone)
                .name(normalizedName)
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .termsVersion(StringUtils.hasText(command.getTermsVersion())
                        ? command.getTermsVersion()
                        : CURRENT_TERMS_VERSION)
                .ipAddress(command.getIpAddress())
                .createdAt(now)
                .expiresAt(now.plus(pendingRegistrationMinutes, ChronoUnit.MINUTES))
                .build());

        phoneVerificationGateway.startVerification(normalizedPhone, phoneChannel());
        auditLogService.record("USER_REGISTRATION_STARTED", null, normalizedEmail,
                "PENDING_REGISTRATION", normalizedEmail);

        return RegistrationView.builder()
                .verificationSentByEmail(false)
                .message("Enviamos um codigo por SMS para " + maskPhone(normalizedPhone)
                        + ". Digite-o para concluir o cadastro.")
                .build();
    }

    @Override
    public AuthenticatedSessionView confirmRegistration(ConfirmRegistrationCommand command) {
        String normalizedEmail = normalizeEmail(command.getEmail());
        PendingRegistration pending = pendingRegistrationGateway.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(
                        "Nao encontramos um cadastro pendente para este e-mail. Comece novamente."));

        if (pending.getExpiresAt() != null && pending.getExpiresAt().isBefore(Instant.now())) {
            pendingRegistrationGateway.deleteByEmail(normalizedEmail);
            throw new BusinessException("O codigo expirou. Comece o cadastro novamente.");
        }

        if (!phoneVerificationGateway.checkVerification(pending.getPhone(), command.getCode())) {
            throw new BusinessException("Codigo invalido ou expirado.");
        }

        userGateway.findByEmail(normalizedEmail).ifPresent(existing -> {
            pendingRegistrationGateway.deleteByEmail(normalizedEmail);
            throw new BusinessException("Ja existe usuario com este e-mail.");
        });
        userGateway.findByPhone(pending.getPhone()).ifPresent(existing -> {
            pendingRegistrationGateway.deleteByEmail(normalizedEmail);
            throw new BusinessException("Ja existe usuario com este telefone.");
        });

        UserProfile user = userGateway.save(UserProfile.builder()
                .name(pending.getName())
                .email(normalizedEmail)
                .passwordHash(pending.getPasswordHash())
                .phone(pending.getPhone())
                .phoneVerified(true)
                .emailVerified(true)
                .buyerRating(4.8)
                .sellerRating(4.8)
                .sellerCredits(operationalCatalogService.initialFreeCredits())
                .purchasedCreditsTotal(0)
                .freeCreditsGranted(true)
                .ipAddress(StringUtils.hasText(command.getIpAddress())
                        ? command.getIpAddress()
                        : pending.getIpAddress())
                .termsAccepted(true)
                .termsAcceptedAt(Instant.now())
                .termsVersion(StringUtils.hasText(pending.getTermsVersion())
                        ? pending.getTermsVersion()
                        : CURRENT_TERMS_VERSION)
                .country("Brasil")
                .build());

        pendingRegistrationGateway.deleteByEmail(normalizedEmail);

        auditLogService.record("USER_REGISTERED", user.getId(), user.getEmail(), "USER", user.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("phoneVerified", true));
        eventPublisherGateway.publish("user.registered", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "phoneVerified", true
        ));

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
    public void startPhoneVerification(String userId, StartPhoneVerificationCommand command) {
        UserProfile user = userGateway.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao encontrado."));
        String normalizedPhone = normalizePhone(command.getPhone());

        userGateway.findByPhone(normalizedPhone)
                .filter(existing -> !Objects.equals(existing.getId(), user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Este telefone ja esta vinculado a outra conta.");
                });

        phoneVerificationGateway.startVerification(normalizedPhone, phoneChannel());
        auditLogService.record("USER_PHONE_VERIFICATION_STARTED", user.getId(), user.getEmail(),
                "USER", user.getId());
    }

    @Override
    public UserProfile confirmPhoneVerification(String userId, ConfirmPhoneVerificationCommand command) {
        UserProfile user = userGateway.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao encontrado."));
        String normalizedPhone = normalizePhone(command.getPhone());

        userGateway.findByPhone(normalizedPhone)
                .filter(existing -> !Objects.equals(existing.getId(), user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Este telefone ja esta vinculado a outra conta.");
                });

        if (!phoneVerificationGateway.checkVerification(normalizedPhone, command.getCode())) {
            throw new BusinessException("Codigo invalido ou expirado.");
        }

        boolean grantCredits = !Boolean.TRUE.equals(user.getFreeCreditsGranted());
        int credits = grantCredits
                ? sellerCreditsOf(user) + operationalCatalogService.initialFreeCredits()
                : sellerCreditsOf(user);

        UserProfile updated = userGateway.save(user.toBuilder()
                .phone(normalizedPhone)
                .phoneVerified(true)
                .sellerCredits(credits)
                .freeCreditsGranted(true)
                .build());

        auditLogService.record("USER_PHONE_VERIFIED", user.getId(), user.getEmail(), "USER", user.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("creditsGranted", grantCredits));
        eventPublisherGateway.publish("user.phone-verified", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "creditsGranted", grantCredits
        ));

        return updated;
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
    public SocialAuthView loginWithGoogle(GoogleLoginCommand command) {
        GoogleIdentityView identity = googleIdentityService.verify(command.getAccessToken());
        String normalizedEmail = normalizeEmail(identity.getEmail());
        validateHmlAccess(normalizedEmail);
        return resolveSocialAuth("google", identity.getSubject(), normalizedEmail,
                socialDisplayName(identity.getName(), identity.getEmail()), command.getIpAddress());
    }

    @Override
    public SocialAuthView loginWithFacebook(FacebookLoginCommand command) {
        FacebookIdentityView identity = facebookIdentityService.verify(command.getAccessToken());
        String normalizedEmail = normalizeEmail(identity.getEmail());
        validateHmlAccess(normalizedEmail);
        return resolveSocialAuth("facebook", identity.getSubject(), normalizedEmail,
                socialDisplayName(identity.getName(), identity.getEmail()), command.getIpAddress());
    }

    /**
     * Decide o desfecho de um login social: se o usuario ja existe e tem telefone verificado,
     * faz login (vinculando o provedor e marcando o e-mail como verificado se preciso). Caso
     * contrario (conta nova ou sem telefone verificado), guarda um cadastro social pendente e
     * exige a verificacao de telefone antes de concluir.
     */
    private SocialAuthView resolveSocialAuth(String provider, String subject, String normalizedEmail,
            String displayName, String ipAddress) {
        Optional<UserProfile> existing = userGateway.findByEmail(normalizedEmail);

        if (existing.isPresent() && existing.get().isPhoneVerified()) {
            UserProfile user = linkSocialProvider(existing.get(), provider, subject);
            return SocialAuthView.ofSession(issueSocialSession(provider, user));
        }

        Instant now = Instant.now();
        PendingSocialAuth pending = pendingSocialAuthGateway.save(PendingSocialAuth.builder()
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .provider(provider)
                .subject(subject)
                .email(normalizedEmail)
                .name(displayName)
                .existingUserId(existing.map(UserProfile::getId).orElse(null))
                .ipAddress(ipAddress)
                .createdAt(now)
                .expiresAt(now.plus(pendingRegistrationMinutes, ChronoUnit.MINUTES))
                .build());

        auditLogService.record("AUTH_SOCIAL_PHONE_REQUIRED", existing.map(UserProfile::getId).orElse(null),
                normalizedEmail, "PENDING_SOCIAL_AUTH", normalizedEmail);

        return SocialAuthView.ofPhoneRequired(pending.getToken());
    }

    @Override
    public void startSocialPhoneVerification(StartSocialPhoneVerificationCommand command) {
        PendingSocialAuth pending = requirePendingSocialAuth(command.getSocialToken());
        String normalizedPhone = normalizePhone(command.getPhone());

        userGateway.findByPhone(normalizedPhone)
                .filter(other -> !Objects.equals(other.getId(), pending.getExistingUserId()))
                .ifPresent(other -> {
                    throw new BusinessException("Este telefone ja esta vinculado a outra conta.");
                });

        phoneVerificationGateway.startVerification(normalizedPhone, phoneChannel());
        auditLogService.record("AUTH_SOCIAL_PHONE_VERIFICATION_STARTED", pending.getExistingUserId(),
                pending.getEmail(), "PENDING_SOCIAL_AUTH", pending.getEmail());
    }

    @Override
    public AuthenticatedSessionView confirmSocialPhoneVerification(ConfirmSocialPhoneVerificationCommand command) {
        PendingSocialAuth pending = requirePendingSocialAuth(command.getSocialToken());
        String normalizedPhone = normalizePhone(command.getPhone());

        userGateway.findByPhone(normalizedPhone)
                .filter(other -> !Objects.equals(other.getId(), pending.getExistingUserId()))
                .ifPresent(other -> {
                    throw new BusinessException("Este telefone ja esta vinculado a outra conta.");
                });

        if (!phoneVerificationGateway.checkVerification(normalizedPhone, command.getCode())) {
            throw new BusinessException("Codigo invalido ou expirado.");
        }

        UserProfile baseUser = StringUtils.hasText(pending.getExistingUserId())
                ? userGateway.findById(pending.getExistingUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."))
                : UserProfile.builder()
                        .email(pending.getEmail())
                        .name(pending.getName())
                        .emailVerified(true)
                        .buyerRating(4.8)
                        .sellerRating(4.8)
                        .sellerCredits(0)
                        .purchasedCreditsTotal(0)
                        .freeCreditsGranted(false)
                        .ipAddress(pending.getIpAddress())
                        .termsAccepted(true)
                        .termsAcceptedAt(Instant.now())
                        .termsVersion(CURRENT_TERMS_VERSION)
                        .country("Brasil")
                        .build();

        boolean grantCredits = !Boolean.TRUE.equals(baseUser.getFreeCreditsGranted());
        int credits = grantCredits
                ? sellerCreditsOf(baseUser) + operationalCatalogService.initialFreeCredits()
                : sellerCreditsOf(baseUser);

        UserProfile.UserProfileBuilder builder = baseUser.toBuilder()
                .emailVerified(true)
                .phone(normalizedPhone)
                .phoneVerified(true)
                .sellerCredits(credits)
                .freeCreditsGranted(true);
        if ("google".equals(pending.getProvider()) && StringUtils.hasText(pending.getSubject())) {
            builder.googleSubject(pending.getSubject());
        }
        if ("facebook".equals(pending.getProvider()) && StringUtils.hasText(pending.getSubject())) {
            builder.facebookSubject(pending.getSubject());
        }

        boolean newAccount = !StringUtils.hasText(baseUser.getId());
        UserProfile user = userGateway.save(builder.build());
        pendingSocialAuthGateway.deleteByToken(pending.getToken());

        if (newAccount) {
            auditLogService.record("USER_REGISTERED", user.getId(), user.getEmail(), "USER", user.getId(),
                    AuditLogService.OUTCOME_SUCCESS, Map.of("provider", pending.getProvider(), "phoneVerified", true));
            eventPublisherGateway.publish("user.registered", Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "phoneVerified", true
            ));
        }
        auditLogService.record("USER_PHONE_VERIFIED", user.getId(), user.getEmail(), "USER", user.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("creditsGranted", grantCredits));

        return issueSocialSession(pending.getProvider(), user);
    }

    private PendingSocialAuth requirePendingSocialAuth(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("Sessao social invalida. Conecte-se novamente.");
        }
        PendingSocialAuth pending = pendingSocialAuthGateway.findByToken(token)
                .orElseThrow(() -> new BusinessException("Sessao social expirada. Conecte-se novamente."));
        if (pending.getExpiresAt() != null && pending.getExpiresAt().isBefore(Instant.now())) {
            pendingSocialAuthGateway.deleteByToken(token);
            throw new BusinessException("Sessao social expirada. Conecte-se novamente.");
        }
        return pending;
    }

    private UserProfile linkSocialProvider(UserProfile existing, String provider, String subject) {
        boolean needsVerification = !existing.isEmailVerified();
        boolean isGoogle = "google".equals(provider);
        boolean isFacebook = "facebook".equals(provider);
        boolean needsLink = StringUtils.hasText(subject)
                && ((isGoogle && !Objects.equals(existing.getGoogleSubject(), subject))
                || (isFacebook && !Objects.equals(existing.getFacebookSubject(), subject)));
        if (!needsVerification && !needsLink) {
            return existing;
        }
        UserProfile.UserProfileBuilder builder = existing.toBuilder().emailVerified(true);
        if (isGoogle && StringUtils.hasText(subject)) {
            builder.googleSubject(subject);
        }
        if (isFacebook && StringUtils.hasText(subject)) {
            builder.facebookSubject(subject);
        }
        return userGateway.save(builder.build());
    }

    private AuthenticatedSessionView issueSocialSession(String provider, UserProfile user) {
        AuthSession session = createSession(user);
        String auditAction = "facebook".equals(provider) ? "AUTH_FACEBOOK_LOGIN" : "AUTH_GOOGLE_LOGIN";
        String event = "facebook".equals(provider) ? "auth.facebook-login" : "auth.google-login";
        auditLogService.record(auditAction, user.getId(), user.getEmail(), "AUTH_SESSION", session.getToken(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("expiresAt", session.getExpiresAt()));
        eventPublisherGateway.publish(event, Map.of(
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

    private String socialDisplayName(String rawName, String rawEmail) {
        String name = normalizeName(rawName);
        if (StringUtils.hasText(name)) {
            return name;
        }
        String email = normalizeEmail(rawEmail);
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

        com.euprocuro.api.domain.model.EmailVerificationToken verificationToken =
                emailVerificationTokenGateway.findByToken(token)
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
            throw new UnauthorizedException("Sessão não informada.");
        }

        AuthSession session = authSessionGateway.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida."));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            authSessionGateway.deleteByToken(token);
            throw new UnauthorizedException("Sessão expirada.");
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

    private String normalizePhone(String value) {
        String digits = Optional.ofNullable(value).orElse("").replaceAll("\\D", "");
        if (digits.startsWith("55") && digits.length() > 11) {
            digits = digits.substring(2);
        }
        if (!digits.matches("\\d{10,11}")) {
            throw new BusinessException("Informe um telefone valido com DDD (ex: (11) 91234-5678).");
        }
        return "+55" + digits;
    }

    private String maskPhone(String phoneE164) {
        if (phoneE164 == null || phoneE164.length() < 4) {
            return "número informado";
        }
        return "•••••-" + phoneE164.substring(phoneE164.length() - 4);
    }

    private PhoneVerificationChannel phoneChannel() {
        try {
            return PhoneVerificationChannel.valueOf(normalizeText(phoneVerificationChannel).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return PhoneVerificationChannel.SMS;
        }
    }

    private int sellerCreditsOf(UserProfile user) {
        return user.getSellerCredits() == null ? 0 : user.getSellerCredits();
    }

    private String buildResetLink(String token) {
        String normalizedBase = resetBaseUrl.endsWith("/") ? resetBaseUrl.substring(0, resetBaseUrl.length() - 1) : resetBaseUrl;
        return normalizedBase + "?mode=reset&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}

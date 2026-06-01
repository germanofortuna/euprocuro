package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.ConfirmPhoneVerificationCommand;
import com.euprocuro.api.application.command.ConfirmRegistrationCommand;
import com.euprocuro.api.application.command.FacebookLoginCommand;
import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.GoogleLoginCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.command.ConfirmSocialPhoneVerificationCommand;
import com.euprocuro.api.application.command.StartPhoneVerificationCommand;
import com.euprocuro.api.application.command.StartRegistrationCommand;
import com.euprocuro.api.application.command.StartSocialPhoneVerificationCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.FacebookIdentityView;
import com.euprocuro.api.application.view.GoogleIdentityView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.application.view.SocialAuthView;
import com.euprocuro.api.domain.gateway.AuthSessionGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EmailVerificationTokenGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.PasswordResetTokenGateway;
import com.euprocuro.api.domain.gateway.PendingRegistrationGateway;
import com.euprocuro.api.domain.gateway.PendingSocialAuthGateway;
import com.euprocuro.api.domain.gateway.PhoneVerificationGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.domain.model.EmailVerificationToken;
import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.domain.model.PendingRegistration;
import com.euprocuro.api.domain.model.PendingSocialAuth;
import com.euprocuro.api.domain.model.PhoneVerificationChannel;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.infrastructure.security.FacebookIdentityService;
import com.euprocuro.api.infrastructure.security.GoogleIdentityService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private AuthSessionGateway authSessionGateway;
    @Mock
    private PasswordResetTokenGateway passwordResetTokenGateway;
    @Mock
    private EmailVerificationTokenGateway emailVerificationTokenGateway;
    @Mock
    private PendingRegistrationGateway pendingRegistrationGateway;
    @Mock
    private PendingSocialAuthGateway pendingSocialAuthGateway;
    @Mock
    private PhoneVerificationGateway phoneVerificationGateway;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private EventPublisherGateway eventPublisherGateway;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private OperationalCatalogService operationalCatalogService;
    @Mock
    private GoogleIdentityService googleIdentityService;
    @Mock
    private FacebookIdentityService facebookIdentityService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "sessionHours", 24L);
        ReflectionTestUtils.setField(authService, "sessionRenewalThresholdHours", 0L);
        ReflectionTestUtils.setField(authService, "passwordResetHours", 2L);
        ReflectionTestUtils.setField(authService, "pendingRegistrationMinutes", 15L);
        ReflectionTestUtils.setField(authService, "phoneVerificationChannel", "SMS");
        ReflectionTestUtils.setField(authService, "resetBaseUrl", "https://app.euprocuro.com");
        ReflectionTestUtils.setField(authService, "exposeResetPreview", true);
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", false);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "");
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", true);
        lenient().when(operationalCatalogService.initialFreeCredits()).thenReturn(15);
    }

    // ----------------------------------------------------------------------
    // startRegistration
    // ----------------------------------------------------------------------

    @Test
    void startRegistrationShouldSavePendingAndSendSms() {
        StartRegistrationCommand command = StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ANA@TESTE.COM")
                .password("Senha123")
                .phone("(11) 91234-5678")
                .ipAddress("192.168.1.100")
                .termsAccepted(true)
                .termsVersion("2026-05-05")
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(pendingRegistrationGateway.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationView result = authService.startRegistration(command);

        assertThat(result.isVerificationSentByEmail()).isFalse();
        assertThat(result.getMessage()).contains("SMS");

        ArgumentCaptor<PendingRegistration> pendingCaptor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationGateway).save(pendingCaptor.capture());
        assertThat(pendingCaptor.getValue().getEmail()).isEqualTo("ana@teste.com");
        assertThat(pendingCaptor.getValue().getPhone()).isEqualTo("+5511912345678");
        assertThat(pendingCaptor.getValue().getPasswordHash()).isEqualTo("senha-hash");
        assertThat(pendingCaptor.getValue().getExpiresAt()).isAfter(Instant.now());
        verify(phoneVerificationGateway).startVerification("+5511912345678", PhoneVerificationChannel.SMS);
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void startRegistrationShouldRejectExistingEmail() {
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
        verify(phoneVerificationGateway, never()).startVerification(any(), any());
    }

    @Test
    void startRegistrationShouldRejectExistingPhone() {
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("telefone");
    }

    @Test
    void startRegistrationShouldRejectInvalidPhone() {
        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("Senha123")
                .phone("123")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("telefone valido");
    }

    @Test
    void startRegistrationShouldRejectWeakPassword() {
        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void startRegistrationShouldRejectIncompleteName() {
        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana")
                .email("ana@teste.com")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nome e sobrenome");
    }

    @Test
    void startRegistrationShouldRejectDisposableEmail() {
        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@mailinator.com")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail permanente");
    }

    @Test
    void startRegistrationShouldRejectInvalidEmailFormat() {
        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("email-invalido")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail valido");
    }

    @Test
    void startRegistrationShouldRejectWhenTermsAreNotAccepted() {
        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(false)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("termos de uso");
    }

    @Test
    void startRegistrationShouldRejectEmailOutsideHmlAllowlistWhenEnabled() {
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", true);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "liberado@teste.com");

        assertThatThrownBy(() -> authService.startRegistration(StartRegistrationCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("Senha123")
                .phone("11912345678")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ambiente restrito");
    }

    // ----------------------------------------------------------------------
    // confirmRegistration
    // ----------------------------------------------------------------------

    @Test
    void confirmRegistrationShouldCreateVerifiedUserWithCreditsAndAutoLogin() {
        PendingRegistration pending = pendingRegistration();
        when(pendingRegistrationGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(pending));
        when(phoneVerificationGateway.checkVerification("+5511912345678", "12345")).thenReturn(true);
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.confirmRegistration(ConfirmRegistrationCommand.builder()
                .email("ANA@TESTE.COM")
                .code("12345")
                .ipAddress("10.0.0.9")
                .build());

        assertThat(result.getToken()).isNotBlank();
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        UserProfile saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("ana@teste.com");
        assertThat(saved.getPhone()).isEqualTo("+5511912345678");
        assertThat(saved.isPhoneVerified()).isTrue();
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getSellerCredits()).isEqualTo(15);
        assertThat(saved.getFreeCreditsGranted()).isTrue();
        assertThat(saved.getPasswordHash()).isEqualTo("senha-hash");
        verify(pendingRegistrationGateway).deleteByEmail("ana@teste.com");
        verify(eventPublisherGateway).publish(eq("user.registered"), any(Map.class));
    }

    @Test
    void confirmRegistrationShouldRejectWhenPendingMissing() {
        when(pendingRegistrationGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.confirmRegistration(ConfirmRegistrationCommand.builder()
                .email("ana@teste.com")
                .code("12345")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cadastro pendente");
    }

    @Test
    void confirmRegistrationShouldRejectAndDeleteExpiredPending() {
        PendingRegistration pending = pendingRegistration().toBuilder()
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();
        when(pendingRegistrationGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authService.confirmRegistration(ConfirmRegistrationCommand.builder()
                .email("ana@teste.com")
                .code("12345")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirou");
        verify(pendingRegistrationGateway).deleteByEmail("ana@teste.com");
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void confirmRegistrationShouldRejectInvalidCode() {
        PendingRegistration pending = pendingRegistration();
        when(pendingRegistrationGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(pending));
        when(phoneVerificationGateway.checkVerification("+5511912345678", "00000")).thenReturn(false);

        assertThatThrownBy(() -> authService.confirmRegistration(ConfirmRegistrationCommand.builder()
                .email("ana@teste.com")
                .code("00000")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Codigo invalido");
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void confirmRegistrationShouldRejectWhenEmailGotTakenMeanwhile() {
        PendingRegistration pending = pendingRegistration();
        when(pendingRegistrationGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(pending));
        when(phoneVerificationGateway.checkVerification("+5511912345678", "12345")).thenReturn(true);
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> authService.confirmRegistration(ConfirmRegistrationCommand.builder()
                .email("ana@teste.com")
                .code("12345")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
        verify(pendingRegistrationGateway).deleteByEmail("ana@teste.com");
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    // ----------------------------------------------------------------------
    // phone verification (lazy credits)
    // ----------------------------------------------------------------------

    @Test
    void startPhoneVerificationShouldSendCode() {
        UserProfile user = baseUser();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());

        authService.startPhoneVerification("user-1", StartPhoneVerificationCommand.builder()
                .phone("11912345678")
                .build());

        verify(phoneVerificationGateway).startVerification("+5511912345678", PhoneVerificationChannel.SMS);
    }

    @Test
    void startPhoneVerificationShouldRejectPhoneFromAnotherUser() {
        UserProfile user = baseUser();
        UserProfile other = baseUser().toBuilder().id("user-2").build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> authService.startPhoneVerification("user-1", StartPhoneVerificationCommand.builder()
                .phone("11912345678")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outra conta");
        verify(phoneVerificationGateway, never()).startVerification(any(), any());
    }

    @Test
    void confirmPhoneVerificationShouldGrantCreditsOnFirstVerification() {
        UserProfile user = baseUser().toBuilder()
                .sellerCredits(0)
                .freeCreditsGranted(false)
                .build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(phoneVerificationGateway.checkVerification("+5511912345678", "12345")).thenReturn(true);
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = authService.confirmPhoneVerification("user-1", ConfirmPhoneVerificationCommand.builder()
                .phone("11912345678")
                .code("12345")
                .build());

        assertThat(result.isPhoneVerified()).isTrue();
        assertThat(result.getPhone()).isEqualTo("+5511912345678");
        assertThat(result.getSellerCredits()).isEqualTo(15);
        assertThat(result.getFreeCreditsGranted()).isTrue();
        verify(eventPublisherGateway).publish(eq("user.phone-verified"), any(Map.class));
    }

    @Test
    void confirmPhoneVerificationShouldNotGrantCreditsTwice() {
        UserProfile user = baseUser().toBuilder()
                .sellerCredits(8)
                .freeCreditsGranted(true)
                .build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(phoneVerificationGateway.checkVerification("+5511912345678", "12345")).thenReturn(true);
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = authService.confirmPhoneVerification("user-1", ConfirmPhoneVerificationCommand.builder()
                .phone("11912345678")
                .code("12345")
                .build());

        assertThat(result.getSellerCredits()).isEqualTo(8);
        assertThat(result.getFreeCreditsGranted()).isTrue();
    }

    @Test
    void confirmPhoneVerificationShouldRejectInvalidCode() {
        UserProfile user = baseUser();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(phoneVerificationGateway.checkVerification("+5511912345678", "00000")).thenReturn(false);

        assertThatThrownBy(() -> authService.confirmPhoneVerification("user-1", ConfirmPhoneVerificationCommand.builder()
                .phone("11912345678")
                .code("00000")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Codigo invalido");
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void confirmPhoneVerificationShouldRejectPhoneFromAnotherUser() {
        UserProfile user = baseUser();
        UserProfile other = baseUser().toBuilder().id("user-2").build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> authService.confirmPhoneVerification("user-1", ConfirmPhoneVerificationCommand.builder()
                .phone("11912345678")
                .code("12345")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outra conta");
    }

    // ----------------------------------------------------------------------
    // login
    // ----------------------------------------------------------------------

    @Test
    void loginShouldReturnSessionWhenCredentialsAreValid() {
        UserProfile user = baseUser();
        user.setPasswordHash("hash");
        user.setEmailVerified(true);

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.login(LoginCommand.builder()
                .email("ana@teste.com")
                .password("123456")
                .build());

        assertThat(result.getUser().getId()).isEqualTo("user-1");
        assertThat(result.getToken()).isNotBlank();
        verify(eventPublisherGateway).publish(eq("auth.login"), any(Map.class));
    }

    @Test
    void requireAuthenticatedSessionShouldRenewExpiringSession() {
        ReflectionTestUtils.setField(authService, "sessionRenewalThresholdHours", 6L);
        AuthSession session = AuthSession.builder()
                .id("session-1")
                .token("token-123")
                .userId("user-1")
                .createdAt(Instant.now().minus(23, ChronoUnit.HOURS))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        UserProfile user = baseUser();

        when(authSessionGateway.findByToken("token-123")).thenReturn(Optional.of(session));
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        AuthenticatedSessionView result = authService.requireAuthenticatedSession("token-123");

        assertThat(result.isRenewed()).isTrue();
        assertThat(result.getUser().getId()).isEqualTo("user-1");
        assertThat(result.getExpiresAt()).isAfter(session.getExpiresAt());

        ArgumentCaptor<AuthSession> sessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
        verify(authSessionGateway).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getToken()).isEqualTo("token-123");
        assertThat(sessionCaptor.getValue().getExpiresAt()).isAfter(session.getExpiresAt());
    }

    @Test
    void requireAuthenticatedSessionShouldKeepFreshSessionUntouched() {
        ReflectionTestUtils.setField(authService, "sessionRenewalThresholdHours", 6L);
        AuthSession session = AuthSession.builder()
                .id("session-1")
                .token("token-123")
                .userId("user-1")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(12, ChronoUnit.HOURS))
                .build();
        UserProfile user = baseUser();

        when(authSessionGateway.findByToken("token-123")).thenReturn(Optional.of(session));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        AuthenticatedSessionView result = authService.requireAuthenticatedSession("token-123");

        assertThat(result.isRenewed()).isFalse();
        assertThat(result.getExpiresAt()).isEqualTo(session.getExpiresAt());
        verify(authSessionGateway, never()).save(any(AuthSession.class));
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        UserProfile user = baseUser();
        user.setPasswordHash("hash");
        user.setEmailVerified(true);

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(LoginCommand.builder()
                .email("ana@teste.com")
                .password("errada")
                .build()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invalidos");
    }

    @Test
    void loginShouldRejectUnverifiedEmail() {
        UserProfile user = baseUser();
        user.setPasswordHash("hash");
        user.setEmailVerified(false);

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(LoginCommand.builder()
                .email("ana@teste.com")
                .password("123456")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Confirme seu e-mail");
    }

    @Test
    void loginShouldRejectEmailOutsideHmlAllowlistWhenEnabled() {
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", true);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "liberado@teste.com");

        assertThatThrownBy(() -> authService.login(LoginCommand.builder()
                .email("ana@teste.com")
                .password("123456")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ambiente restrito");
    }

    // ----------------------------------------------------------------------
    // social login (no free credits on creation anymore)
    // ----------------------------------------------------------------------

    @Test
    void googleLoginShouldRequirePhoneAndNotCreateUserForNewAccount() {
        when(googleIdentityService.verify("google-token")).thenReturn(GoogleIdentityView.builder()
                .subject("google-sub")
                .email("NOVA@TESTE.COM")
                .name("Nova Pessoa")
                .emailVerified(true)
                .build());
        when(userGateway.findByEmail("nova@teste.com")).thenReturn(Optional.empty());
        when(pendingSocialAuthGateway.save(any(PendingSocialAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocialAuthView result = authService.loginWithGoogle(GoogleLoginCommand.builder()
                .accessToken("google-token")
                .ipAddress("10.0.0.1")
                .build());

        assertThat(result.isPhoneRequired()).isTrue();
        assertThat(result.getSocialToken()).isNotBlank();
        ArgumentCaptor<PendingSocialAuth> captor = ArgumentCaptor.forClass(PendingSocialAuth.class);
        verify(pendingSocialAuthGateway).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo("google");
        assertThat(captor.getValue().getEmail()).isEqualTo("nova@teste.com");
        assertThat(captor.getValue().getExistingUserId()).isNull();
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(authSessionGateway, never()).save(any(AuthSession.class));
    }

    @Test
    void googleLoginShouldLoginExistingUserWithVerifiedPhone() {
        UserProfile existing = baseUser().toBuilder()
                .email("ana@teste.com")
                .emailVerified(true)
                .phoneVerified(true)
                .googleSubject(null)
                .build();
        when(googleIdentityService.verify("google-token")).thenReturn(GoogleIdentityView.builder()
                .subject("google-sub")
                .email("ana@teste.com")
                .name("Ana Silva")
                .emailVerified(true)
                .build());
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(existing));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocialAuthView result = authService.loginWithGoogle(GoogleLoginCommand.builder()
                .accessToken("google-token")
                .build());

        assertThat(result.isPhoneRequired()).isFalse();
        assertThat(result.getSession().getUser().getGoogleSubject()).isEqualTo("google-sub");
        verify(eventPublisherGateway).publish(eq("auth.google-login"), any(Map.class));
        verify(pendingSocialAuthGateway, never()).save(any(PendingSocialAuth.class));
    }

    @Test
    void googleLoginShouldRequirePhoneForExistingUserWithoutVerifiedPhone() {
        UserProfile existing = baseUser().toBuilder()
                .email("ana@teste.com")
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        when(googleIdentityService.verify("google-token")).thenReturn(GoogleIdentityView.builder()
                .subject("google-sub")
                .email("ana@teste.com")
                .name("Ana Silva")
                .emailVerified(true)
                .build());
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(existing));
        when(pendingSocialAuthGateway.save(any(PendingSocialAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocialAuthView result = authService.loginWithGoogle(GoogleLoginCommand.builder()
                .accessToken("google-token")
                .build());

        assertThat(result.isPhoneRequired()).isTrue();
        ArgumentCaptor<PendingSocialAuth> captor = ArgumentCaptor.forClass(PendingSocialAuth.class);
        verify(pendingSocialAuthGateway).save(captor.capture());
        assertThat(captor.getValue().getExistingUserId()).isEqualTo("user-1");
        verify(authSessionGateway, never()).save(any(AuthSession.class));
    }

    @Test
    void facebookLoginShouldRequirePhoneAndNotCreateUserForNewAccount() {
        when(facebookIdentityService.verify("fb-token")).thenReturn(FacebookIdentityView.builder()
                .subject("fb-sub")
                .email("NOVA@TESTE.COM")
                .name("Nova Pessoa")
                .emailVerified(true)
                .build());
        when(userGateway.findByEmail("nova@teste.com")).thenReturn(Optional.empty());
        when(pendingSocialAuthGateway.save(any(PendingSocialAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocialAuthView result = authService.loginWithFacebook(FacebookLoginCommand.builder()
                .accessToken("fb-token")
                .ipAddress("10.0.0.1")
                .build());

        assertThat(result.isPhoneRequired()).isTrue();
        assertThat(result.getSocialToken()).isNotBlank();
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(authSessionGateway, never()).save(any(AuthSession.class));
    }

    @Test
    void facebookLoginShouldLoginExistingUserWithVerifiedPhone() {
        UserProfile existing = baseUser().toBuilder()
                .email("ana@teste.com")
                .emailVerified(true)
                .phoneVerified(true)
                .facebookSubject("fb-sub")
                .build();
        when(facebookIdentityService.verify("fb-token")).thenReturn(FacebookIdentityView.builder()
                .subject("fb-sub")
                .email("ana@teste.com")
                .name("Ana Silva")
                .emailVerified(true)
                .build());
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(existing));
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocialAuthView result = authService.loginWithFacebook(FacebookLoginCommand.builder()
                .accessToken("fb-token")
                .build());

        assertThat(result.isPhoneRequired()).isFalse();
        assertThat(result.getSession().getUser().getFacebookSubject()).isEqualTo("fb-sub");
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(eventPublisherGateway).publish(eq("auth.facebook-login"), any(Map.class));
    }

    @Test
    void startSocialPhoneVerificationShouldSendCode() {
        PendingSocialAuth pending = pendingSocialAuth(null);
        when(pendingSocialAuthGateway.findByToken("social-token")).thenReturn(Optional.of(pending));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());

        authService.startSocialPhoneVerification(StartSocialPhoneVerificationCommand.builder()
                .socialToken("social-token")
                .phone("11912345678")
                .build());

        verify(phoneVerificationGateway).startVerification("+5511912345678", PhoneVerificationChannel.SMS);
    }

    @Test
    void startSocialPhoneVerificationShouldRejectExpiredToken() {
        PendingSocialAuth pending = pendingSocialAuth(null).toBuilder()
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();
        when(pendingSocialAuthGateway.findByToken("social-token")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authService.startSocialPhoneVerification(StartSocialPhoneVerificationCommand.builder()
                .socialToken("social-token")
                .phone("11912345678")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirada");
        verify(phoneVerificationGateway, never()).startVerification(any(), any());
    }

    @Test
    void confirmSocialPhoneVerificationShouldCreateNewUserWithCreditsAndSession() {
        PendingSocialAuth pending = pendingSocialAuth(null);
        when(pendingSocialAuthGateway.findByToken("social-token")).thenReturn(Optional.of(pending));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(phoneVerificationGateway.checkVerification("+5511912345678", "12345")).thenReturn(true);
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("google-user");
            return user;
        });
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.confirmSocialPhoneVerification(ConfirmSocialPhoneVerificationCommand.builder()
                .socialToken("social-token")
                .phone("11912345678")
                .code("12345")
                .build());

        assertThat(result.getToken()).isNotBlank();
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        UserProfile saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("nova@teste.com");
        assertThat(saved.getGoogleSubject()).isEqualTo("google-sub");
        assertThat(saved.isPhoneVerified()).isTrue();
        assertThat(saved.getSellerCredits()).isEqualTo(15);
        assertThat(saved.getFreeCreditsGranted()).isTrue();
        verify(pendingSocialAuthGateway).deleteByToken("social-token");
        verify(eventPublisherGateway).publish(eq("user.registered"), any(Map.class));
    }

    @Test
    void confirmSocialPhoneVerificationShouldGrantCreditsToExistingUserOnce() {
        PendingSocialAuth pending = pendingSocialAuth("user-1").toBuilder().provider("facebook").subject("fb-sub").build();
        UserProfile existing = baseUser().toBuilder()
                .sellerCredits(0)
                .freeCreditsGranted(false)
                .build();
        when(pendingSocialAuthGateway.findByToken("social-token")).thenReturn(Optional.of(pending));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(userGateway.findById("user-1")).thenReturn(Optional.of(existing));
        when(phoneVerificationGateway.checkVerification("+5511912345678", "12345")).thenReturn(true);
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.confirmSocialPhoneVerification(ConfirmSocialPhoneVerificationCommand.builder()
                .socialToken("social-token")
                .phone("11912345678")
                .code("12345")
                .build());

        assertThat(result.getUser().getSellerCredits()).isEqualTo(15);
        assertThat(result.getUser().getFacebookSubject()).isEqualTo("fb-sub");
        assertThat(result.getUser().isPhoneVerified()).isTrue();
        verify(eventPublisherGateway).publish(eq("auth.facebook-login"), any(Map.class));
        verify(eventPublisherGateway, never()).publish(eq("user.registered"), any(Map.class));
    }

    @Test
    void confirmSocialPhoneVerificationShouldRejectInvalidCode() {
        PendingSocialAuth pending = pendingSocialAuth(null);
        when(pendingSocialAuthGateway.findByToken("social-token")).thenReturn(Optional.of(pending));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.empty());
        when(phoneVerificationGateway.checkVerification("+5511912345678", "00000")).thenReturn(false);

        assertThatThrownBy(() -> authService.confirmSocialPhoneVerification(ConfirmSocialPhoneVerificationCommand.builder()
                .socialToken("social-token")
                .phone("11912345678")
                .code("00000")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Codigo invalido");
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void confirmSocialPhoneVerificationShouldRejectPhoneFromAnotherUser() {
        PendingSocialAuth pending = pendingSocialAuth(null);
        UserProfile other = baseUser().toBuilder().id("user-2").build();
        when(pendingSocialAuthGateway.findByToken("social-token")).thenReturn(Optional.of(pending));
        when(userGateway.findByPhone("+5511912345678")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> authService.confirmSocialPhoneVerification(ConfirmSocialPhoneVerificationCommand.builder()
                .socialToken("social-token")
                .phone("11912345678")
                .code("12345")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outra conta");
    }

    // ----------------------------------------------------------------------
    // me / logout / delete
    // ----------------------------------------------------------------------

    @Test
    void meByUserIdShouldReturnCurrentUserData() {
        UserProfile user = baseUser();

        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        UserProfile result = authService.meByUserId("user-1");

        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo("ana@teste.com");
    }

    @Test
    void logoutShouldDeleteSessionAndPublishEvent() {
        UserProfile user = baseUser();
        AuthSession session = AuthSession.builder()
                .token("token-123")
                .userId("user-1")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();

        when(authSessionGateway.findByToken("token-123")).thenReturn(Optional.of(session));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        authService.logout("token-123");

        verify(authSessionGateway).deleteByToken("token-123");
        verify(eventPublisherGateway).publish(eq("auth.logout"), any(Map.class));
    }

    @Test
    void requireAuthenticatedUserShouldRejectExpiredSession() {
        AuthSession session = AuthSession.builder()
                .token("token-123")
                .userId("user-1")
                .createdAt(Instant.now().minus(3, ChronoUnit.HOURS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        when(authSessionGateway.findByToken("token-123")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.requireAuthenticatedUser("token-123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expirada");

        verify(authSessionGateway).deleteByToken("token-123");
    }

    // ----------------------------------------------------------------------
    // forgot / reset password
    // ----------------------------------------------------------------------

    @Test
    void forgotPasswordShouldReturnPreviewWhenEmailDeliveryFailsAndPreviewIsEnabled() {
        UserProfile user = baseUser();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenGateway.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailGateway.sendPasswordResetEmail(eq(user), any(String.class))).thenReturn(false);

        PasswordResetRequestView result = authService.forgotPassword(ForgotPasswordCommand.builder()
                .email("ana@teste.com")
                .build());

        assertThat(result.getPreviewToken()).isNotBlank();
        assertThat(result.getPreviewResetLink()).contains("https://app.euprocuro.com?mode=reset&token=");
        verify(eventPublisherGateway).publish(eq("auth.password-reset-requested"), any(Map.class));
    }

    @Test
    void forgotPasswordShouldSilentlyAcceptUnknownEmail() {
        when(userGateway.findByEmail("missing@teste.com")).thenReturn(Optional.empty());

        PasswordResetRequestView result = authService.forgotPassword(ForgotPasswordCommand.builder()
                .email("missing@teste.com")
                .build());

        assertThat(result.getMessage()).contains("Se o e-mail existir");
        assertThat(result.getPreviewResetLink()).isNull();
    }

    @Test
    void forgotPasswordShouldHidePreviewWhenDisabled() {
        ReflectionTestUtils.setField(authService, "exposeResetPreview", false);
        UserProfile user = baseUser();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenGateway.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailGateway.sendPasswordResetEmail(eq(user), any(String.class))).thenReturn(false);

        PasswordResetRequestView result = authService.forgotPassword(ForgotPasswordCommand.builder()
                .email("ana@teste.com")
                .build());

        assertThat(result.getPreviewToken()).isNull();
        assertThat(result.getPreviewResetLink()).isNull();
    }

    @Test
    void resetPasswordShouldPersistNewHashAndMarkTokenAsUsed() {
        UserProfile user = baseUser();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("reset-123")
                .userId("user-1")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(passwordResetTokenGateway.findByToken("reset-123")).thenReturn(Optional.of(resetToken));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nova1234")).thenReturn("nova-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenGateway.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPassword(ResetPasswordCommand.builder()
                .token("reset-123")
                .newPassword("nova1234")
                .confirmPassword("nova1234")
                .build());

        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(userGateway).save(userCaptor.capture());
        verify(passwordResetTokenGateway).save(tokenCaptor.capture());
        verify(eventPublisherGateway).publish(eq("auth.password-reset-completed"), any(Map.class));

        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("nova-hash");
        assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();
    }

    @Test
    void resetPasswordShouldRejectBlankToken() {
        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordCommand.builder()
                .token(" ")
                .newPassword("nova1234")
                .confirmPassword("nova1234")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token de redefinicao invalido");
    }

    @Test
    void resetPasswordShouldRejectUnknownToken() {
        when(passwordResetTokenGateway.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordCommand.builder()
                .token("missing")
                .newPassword("nova1234")
                .confirmPassword("nova1234")
                .build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Token de redefinicao nao encontrado");
    }

    @Test
    void resetPasswordShouldRejectExpiredToken() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("reset-123")
                .userId("user-1")
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        when(passwordResetTokenGateway.findByToken("reset-123")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordCommand.builder()
                .token("reset-123")
                .newPassword("nova1234")
                .confirmPassword("nova1234")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    void resetPasswordShouldRejectMismatchedConfirmation() {
        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordCommand.builder()
                .token("reset-123")
                .newPassword("nova1234")
                .confirmPassword("outra123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmacao");
    }

    @Test
    void resetPasswordShouldRejectUsedToken() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("reset-123")
                .userId("user-1")
                .usedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(passwordResetTokenGateway.findByToken("reset-123")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordCommand.builder()
                .token("reset-123")
                .newPassword("nova1234")
                .confirmPassword("nova1234")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ja foi utilizado");
    }

    // ----------------------------------------------------------------------
    // verifyEmail
    // ----------------------------------------------------------------------

    @Test
    void verifyEmailShouldMarkUserAsVerifiedAndUseToken() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("verify-123")
                .userId("user-1")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        UserProfile user = baseUser().toBuilder()
                .emailVerified(false)
                .build();

        when(emailVerificationTokenGateway.findByToken("verify-123")).thenReturn(Optional.of(token));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerificationTokenGateway.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.verifyEmail("verify-123");

        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        verify(emailVerificationTokenGateway).save(any(EmailVerificationToken.class));
        verify(eventPublisherGateway).publish(eq("auth.email-verified"), any(Map.class));
    }

    @Test
    void verifyEmailShouldRejectBlankToken() {
        assertThatThrownBy(() -> authService.verifyEmail(" "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token de verificacao invalido");
    }

    @Test
    void verifyEmailShouldRejectUnknownToken() {
        when(emailVerificationTokenGateway.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Token de verificacao nao encontrado");
    }

    @Test
    void verifyEmailShouldAcceptUsedTokenWhenUserIsAlreadyVerified() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("verify-123")
                .userId("user-1")
                .usedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        UserProfile user = baseUser().toBuilder()
                .emailVerified(true)
                .build();

        when(emailVerificationTokenGateway.findByToken("verify-123")).thenReturn(Optional.of(token));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        authService.verifyEmail("verify-123");

        verify(userGateway, never()).save(any(UserProfile.class));
        verify(emailVerificationTokenGateway, never()).save(any(EmailVerificationToken.class));
        verify(eventPublisherGateway, never()).publish(eq("auth.email-verified"), any(Map.class));
    }

    @Test
    void verifyEmailShouldRejectUsedTokenWhenUserIsNotVerified() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("verify-123")
                .userId("user-1")
                .usedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        UserProfile user = baseUser().toBuilder()
                .emailVerified(false)
                .build();

        when(emailVerificationTokenGateway.findByToken("verify-123")).thenReturn(Optional.of(token));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail("verify-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ja foi verificado");
    }

    @Test
    void verifyEmailShouldRejectMissingUser() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("verify-123")
                .userId("missing-user")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(emailVerificationTokenGateway.findByToken("verify-123")).thenReturn(Optional.of(token));
        when(userGateway.findById("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("verify-123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario nao encontrado");
    }

    @Test
    void verifyEmailShouldRejectExpiredToken() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("verify-123")
                .userId("user-1")
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        when(emailVerificationTokenGateway.findByToken("verify-123")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("verify-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    void requireAuthenticatedUserShouldRejectBlankToken() {
        assertThatThrownBy(() -> authService.requireAuthenticatedUser(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessão não informada");
    }

    @Test
    void requireAuthenticatedUserShouldRejectMissingUserForValidSession() {
        AuthSession session = AuthSession.builder()
                .token("token-123")
                .userId("missing-user")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(authSessionGateway.findByToken("token-123")).thenReturn(Optional.of(session));
        when(userGateway.findById("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.requireAuthenticatedUser("token-123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessão inválida");
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private PendingRegistration pendingRegistration() {
        Instant now = Instant.now();
        return PendingRegistration.builder()
                .id("pending-1")
                .email("ana@teste.com")
                .phone("+5511912345678")
                .name("Ana Silva")
                .passwordHash("senha-hash")
                .termsVersion("2026-05-05")
                .createdAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .build();
    }

    private UserProfile baseUser() {
        return UserProfile.builder()
                .id("user-1")
                .name("Ana Silva")
                .email("ana@teste.com")
                .passwordHash("hash")
                .buyerRating(4.8)
                .sellerRating(4.9)
                .build();
    }

    private PendingSocialAuth pendingSocialAuth(String existingUserId) {
        Instant now = Instant.now();
        return PendingSocialAuth.builder()
                .id("pending-social-1")
                .token("social-token")
                .provider("google")
                .subject("google-sub")
                .email("nova@teste.com")
                .name("Nova Pessoa")
                .existingUserId(existingUserId)
                .createdAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .build();
    }
}

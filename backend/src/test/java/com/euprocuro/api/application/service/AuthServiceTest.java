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

import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.RegisterUserCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.domain.gateway.AuthSessionGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EmailVerificationTokenGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.PasswordResetTokenGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.domain.model.EmailVerificationToken;
import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.domain.model.UserProfile;

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
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private EventPublisherGateway eventPublisherGateway;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private OperationalCatalogService operationalCatalogService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "sessionHours", 24L);
        ReflectionTestUtils.setField(authService, "sessionRenewalThresholdHours", 0L);
        ReflectionTestUtils.setField(authService, "passwordResetHours", 2L);
        ReflectionTestUtils.setField(authService, "emailVerificationHours", 24L);
        ReflectionTestUtils.setField(authService, "resetBaseUrl", "https://app.euprocuro.com");
        ReflectionTestUtils.setField(authService, "exposeResetPreview", true);
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", false);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "");
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", true);
        lenient().when(operationalCatalogService.initialFreeCredits()).thenReturn(15);
    }

    @Test
    void registerShouldCreateUnverifiedUserAndSendVerificationEmail() {
        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .city("Sao Paulo")
                .state("SP")
                .ipAddress("192.168.1.100")
                .termsAccepted(true)
                .termsVersion("2026-05-05")
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(emailVerificationTokenGateway.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailGateway.sendEmailVerificationEmail(any(UserProfile.class), any(String.class))).thenReturn(true);

        RegistrationView result = authService.register(command);

        assertThat(result.isVerificationSentByEmail()).isTrue();
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getIpAddress()).isEqualTo("192.168.1.100");  // <- Novo
        assertThat(userCaptor.getValue().isTermsAccepted()).isTrue();
        assertThat(userCaptor.getValue().getTermsAcceptedAt()).isNotNull();
        assertThat(userCaptor.getValue().getTermsVersion()).isEqualTo("2026-05-05");
    }

    @Test
    void registerShouldReturnVerificationSentMessageWhenEmailIsDelivered() {
        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .city("Sao Paulo")
                .state("SP")
                .ipAddress("192.168.1.100")  // <- Adicionado
                .termsAccepted(true)
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(emailVerificationTokenGateway.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailGateway.sendEmailVerificationEmail(any(UserProfile.class), any(String.class))).thenReturn(true);

        RegistrationView result = authService.register(command);

        assertThat(result.isVerificationSentByEmail()).isTrue();
        assertThat(result.getMessage()).contains("Enviamos um link");
    }

    @Test
    void registerShouldAcceptValidCnpj() {
        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Loja Teste")
                .email("loja@teste.com")
                .documentNumber("11.222.333/0001-81")
                .password("Senha123")
                .city("Erechim")
                .state("rs")
                .termsAccepted(true)
                .build();

        when(userGateway.findByEmail("loja@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("11222333000181")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-cnpj");
            return user;
        });
        when(emailVerificationTokenGateway.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailGateway.sendEmailVerificationEmail(any(UserProfile.class), any(String.class))).thenReturn(true);

        RegistrationView result = authService.register(command);

        assertThat(result.getMessage()).contains("Conta criada");
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getDocumentNumber()).isEqualTo("11222333000181");
        assertThat(userCaptor.getValue().getDocumentType()).isEqualTo("CNPJ");
        assertThat(userCaptor.getValue().getState()).isEqualTo("RS");
        verify(eventPublisherGateway).publish(eq("user.registered"), any(Map.class));
    }

    @Test
    void registerShouldRollbackUserWhenVerificationEmailFails() {
        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .city("Sao Paulo")
                .state("SP")
                .termsAccepted(true)
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(emailVerificationTokenGateway.save(any(EmailVerificationToken.class))).thenReturn(EmailVerificationToken.builder()
                .token("verify-123")
                .userId("user-1")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build());
        when(emailGateway.sendEmailVerificationEmail(any(UserProfile.class), any(String.class))).thenReturn(false);

        assertThatThrownBy(() -> authService.register(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail de confirma");

        verify(emailVerificationTokenGateway).deleteByToken("verify-123");
        verify(userGateway).deleteById("user-1");
        verify(eventPublisherGateway, never()).publish(eq("user.registered"), any(Map.class));
    }

    @Test
    void registerShouldSkipVerificationEmailWhenNotRequired() {
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", false);
        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .city("Sao Paulo")
                .state("SP")
                .termsAccepted(true)
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });

        RegistrationView result = authService.register(command);

        assertThat(result.isVerificationSentByEmail()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Conta criada");
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        verify(emailVerificationTokenGateway, never()).save(any(EmailVerificationToken.class));
        verify(emailGateway, never()).sendEmailVerificationEmail(any(UserProfile.class), any(String.class));
        verify(eventPublisherGateway).publish(eq("user.registered"), any(Map.class));
    }

    @Test
    void registerShouldRejectWhenTermsAreNotAccepted() {
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .password("Senha123")
                .city("Sao Paulo")
                .state("SP")
                .termsAccepted(false)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("termos de uso");
    }

    @Test
    void registerShouldRejectWeakPassword() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .password("123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ja existe usuario");
    }

    @Test
    void registerShouldRejectDuplicateDocument() {
        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CPF/CNPJ");
    }

    @Test
    void registerShouldRejectInvalidDocument() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("111.111.111-11")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CPF ou CNPJ valido");
    }

    @Test
    void registerShouldRejectInvalidEmailFormat() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("email-invalido")
                .documentNumber("52998224725")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail valido");
    }

    @Test
    void registerShouldRejectStateWithMoreThanTwoLetters() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .password("Senha123")
                .state("SPO")
                .termsAccepted(true)
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UF com 2 letras");
    }

    @Test
    void registerShouldRejectCnpjWithRepeatedDigits() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Loja Teste")
                .email("loja@teste.com")
                .documentNumber("11.111.111/1111-11")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CPF ou CNPJ valido");
    }

    @Test
    void registerShouldRejectDisposableEmail() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@mailinator.com")
                .documentNumber("52998224725")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail permanente");
    }

    @Test
    void registerShouldRejectEmailOutsideHmlAllowlistWhenEnabled() {
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", true);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "liberado@teste.com, outro@teste.com");

        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ambiente restrito");
    }

    @Test
    void registerShouldAcceptEmailInsideHmlAllowlistWhenEnabled() {
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", true);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "liberado@teste.com, ana@teste.com");

        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ANA@TESTE.COM")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .termsAccepted(true)
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-allowlist");
            return user;
        });
        when(emailVerificationTokenGateway.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailGateway.sendEmailVerificationEmail(any(UserProfile.class), any(String.class))).thenReturn(true);

        RegistrationView result = authService.register(command);

        assertThat(result.getMessage()).contains("Conta criada");
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userGateway).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("ana@teste.com");
    }

    @Test
    void registerShouldRejectIncompleteName() {
        assertThatThrownBy(() -> authService.register(RegisterUserCommand.builder()
                .name("Ana")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .password("Senha123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nome e sobrenome");
    }

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

    @Test
    void requireAuthenticatedUserShouldRejectBlankToken() {
        assertThatThrownBy(() -> authService.requireAuthenticatedUser(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessao nao informada");
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
                .hasMessageContaining("Sessao invalida");
    }

    private UserProfile baseUser() {
        return UserProfile.builder()
                .id("user-1")
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("52998224725")
                .documentType("CPF")
                .passwordHash("hash")
                .city("Sao Paulo")
                .state("SP")
                .buyerRating(4.8)
                .sellerRating(4.9)
                .build();
    }
}

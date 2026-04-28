package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.euprocuro.api.application.exception.UnauthorizedException;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private AuthSessionGateway authSessionGateway;
    @Mock
    private PasswordResetTokenGateway passwordResetTokenGateway;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private EventPublisherGateway eventPublisherGateway;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "sessionHours", 24L);
        ReflectionTestUtils.setField(authService, "passwordResetHours", 2L);
        ReflectionTestUtils.setField(authService, "resetBaseUrl", "https://app.euprocuro.com");
        ReflectionTestUtils.setField(authService, "exposeResetPreview", true);
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", false);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "");
    }

    @Test
    void registerShouldCreateEncodedUserAndSession() {
        RegisterUserCommand command = RegisterUserCommand.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .documentNumber("529.982.247-25")
                .password("Senha123")
                .city("Sao Paulo")
                .state("SP")
                .bio("Compradora")
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.register(command);

        assertThat(result.getUser().getId()).isEqualTo("user-1");
        assertThat(result.getUser().getEmail()).isEqualTo("ana@teste.com");
        assertThat(result.getUser().getDocumentNumber()).isEqualTo("52998224725");
        assertThat(result.getUser().getDocumentType()).isEqualTo("CPF");
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        verify(eventPublisherGateway).publish(eq("user.registered"), any(Map.class));
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
                .build();

        when(userGateway.findByEmail("loja@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("11222333000181")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-cnpj");
            return user;
        });
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.register(command);

        assertThat(result.getUser().getId()).isEqualTo("user-cnpj");
        assertThat(result.getUser().getDocumentNumber()).isEqualTo("11222333000181");
        assertThat(result.getUser().getDocumentType()).isEqualTo("CNPJ");
        assertThat(result.getUser().getState()).isEqualTo("RS");
        assertThat(result.getToken()).isNotBlank();
        verify(eventPublisherGateway).publish(eq("user.registered"), any(Map.class));
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
                .hasMessageContaining("homologacao");
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
                .build();

        when(userGateway.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(userGateway.findByDocumentNumber("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("senha-hash");
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile user = invocation.getArgument(0);
            user.setId("user-allowlist");
            return user;
        });
        when(authSessionGateway.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedSessionView result = authService.register(command);

        assertThat(result.getUser().getEmail()).isEqualTo("ana@teste.com");
        assertThat(result.getToken()).isNotBlank();
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
    void loginShouldRejectInvalidPassword() {
        UserProfile user = baseUser();
        user.setPasswordHash("hash");

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
    void loginShouldRejectEmailOutsideHmlAllowlistWhenEnabled() {
        ReflectionTestUtils.setField(authService, "hmlAccessEnabled", true);
        ReflectionTestUtils.setField(authService, "hmlAllowedEmails", "liberado@teste.com");

        assertThatThrownBy(() -> authService.login(LoginCommand.builder()
                .email("ana@teste.com")
                .password("123456")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("homologacao");
    }

    @Test
    void meShouldReturnCurrentSessionData() {
        UserProfile user = baseUser();
        AuthSession session = AuthSession.builder()
                .token("token-123")
                .userId("user-1")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();

        when(authSessionGateway.findByToken("token-123")).thenReturn(Optional.of(session));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        AuthenticatedSessionView result = authService.me("token-123");

        assertThat(result.getToken()).isEqualTo("token-123");
        assertThat(result.getUser().getEmail()).isEqualTo("ana@teste.com");
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
                .bio("Compradora")
                .buyerRating(4.8)
                .sellerRating(4.9)
                .build();
    }
}

package com.euprocuro.api.entrypoints.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.euprocuro.api.entrypoints.rest.dto.response.MeResponse;
import com.euprocuro.api.entrypoints.rest.security.AuthTokenResolver;
import com.euprocuro.api.entrypoints.rest.security.ClientIpResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.euprocuro.api.application.service.AdminAccessService;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.ForgotPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.GoogleLoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.LoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.RegisterRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ResetPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.ActionMessageResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.AuthResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.AuthCookieManager;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;
import com.euprocuro.api.infrastructure.security.TurnstileVerificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Endpoints relacionados a autenticacao e gerenciamento de conta do usuario.")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthCookieManager authCookieManager;
    private final AuthTokenResolver authTokenResolver;
    private final ClientIpResolver clientIpResolver;
    private final TurnstileVerificationService turnstileVerificationService;
    private final AdminAccessService adminAccessService;

    @Value("${application.auth.expose-session-token:false}")
    private boolean exposeSessionToken;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ActionMessageResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        turnstileVerificationService.verify(request.getTurnstileToken(), clientIp);
        return RestMapper.toResponse(
                authUseCase.register(RestMapper.toCommand(request, clientIp))
        );
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        turnstileVerificationService.verify(request.getTurnstileToken(), clientIpResolver.resolve(httpRequest));
        return toCookieAuthResponse(authUseCase.login(RestMapper.toCommand(request)), response);
    }

    @PostMapping("/google")
    public AuthResponse googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        turnstileVerificationService.verify(request.getTurnstileToken(), clientIp);
        return toCookieAuthResponse(
                authUseCase.loginWithGoogle(RestMapper.toGoogleLoginCommand(request, clientIp)),
                response
        );
    }

    @GetMapping("/me")
    public MeResponse me(HttpServletRequest request) {
        String userId = CurrentUserContext.userId(request);
        com.euprocuro.api.domain.model.UserProfile user = authUseCase.meByUserId(userId);
        MeResponse response = RestMapper.toMeResponse(user);
        response.setAdmin(adminAccessService.isAdmin(user.getEmail()));
        CurrentUserContext.optionalSessionExpiresAt(request).ifPresent(response::setExpiresAt);
        return response;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authTokenResolver.resolve(request)
                .ifPresent(authUseCase::logoutIfPresent);
        authCookieManager.clearSessionCookie(response);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(HttpServletRequest request, HttpServletResponse response) {
        authUseCase.deleteCurrentUser(CurrentUserContext.userId(request));
        authCookieManager.clearSessionCookie(response);
    }

    @PostMapping("/forgot-password")
    public ActionMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        turnstileVerificationService.verify(request.getTurnstileToken(), clientIpResolver.resolve(httpRequest));
        return RestMapper.toResponse(authUseCase.forgotPassword(RestMapper.toCommand(request)));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(RestMapper.toCommand(request));
    }

    @GetMapping("/verify-email")
    public ActionMessageResponse verifyEmail(@RequestParam String token) {
        authUseCase.verifyEmail(token);
        return ActionMessageResponse.builder()
                .message("E-mail verificado com sucesso.")
                .build();
    }

    private AuthResponse toCookieAuthResponse(
        com.euprocuro.api.application.view.AuthenticatedSessionView session,
        HttpServletResponse response
    ) {
        authCookieManager.writeSessionCookie(response, session.getToken(), session.getExpiresAt());

        return AuthResponse.builder()
                .token(exposeSessionToken ? session.getToken() : null)
                .expiresAt(session.getExpiresAt())
                .user(RestMapper.toResponse(session.getUser(), adminAccessService.isAdmin(session.getUser().getEmail())))
                .build();
    }
}

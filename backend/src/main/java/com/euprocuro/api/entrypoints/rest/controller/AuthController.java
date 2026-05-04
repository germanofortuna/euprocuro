package com.euprocuro.api.entrypoints.rest.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.euprocuro.api.entrypoints.rest.dto.response.MeResponse;
import com.euprocuro.api.entrypoints.rest.security.AuthTokenResolver;
import com.euprocuro.api.entrypoints.rest.security.ClientIpResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.ForgotPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.LoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.RegisterRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ResetPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.ActionMessageResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.AuthResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.AuthCookieManager;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

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

    @Value("${application.auth.expose-session-token:false}")
    private boolean exposeSessionToken;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ActionMessageResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        return RestMapper.toResponse(
                authUseCase.register(RestMapper.toCommand(request, clientIp))
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return toCookieAuthResponse(authUseCase.login(RestMapper.toCommand(request)), response);
    }

    @GetMapping("/me")
    public MeResponse me(HttpServletRequest request) {
        String userId = CurrentUserContext.userId(request);
        return RestMapper.toMeResponse(authUseCase.meByUserId(userId));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authTokenResolver.resolve(request)
                .ifPresent(authUseCase::logoutIfPresent);
        authCookieManager.clearSessionCookie(response);
    }

    @PostMapping("/forgot-password")
    public ActionMessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
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
        response.addHeader("X-Auth-Expose-Session-Token", String.valueOf(exposeSessionToken));

        return AuthResponse.builder()
                .token(exposeSessionToken ? session.getToken() : null)
                .expiresAt(session.getExpiresAt())
                .user(RestMapper.toResponse(session.getUser()))
                .build();
    }
}
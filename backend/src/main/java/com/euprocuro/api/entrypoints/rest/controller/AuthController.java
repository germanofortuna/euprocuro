package com.euprocuro.api.entrypoints.rest.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthCookieManager authCookieManager;

    @Value("${application.auth.expose-session-token:true}")
    private boolean exposeSessionToken;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return toCookieAuthResponse(authUseCase.register(RestMapper.toCommand(request)), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return toCookieAuthResponse(authUseCase.login(RestMapper.toCommand(request)), response);
    }

    @GetMapping("/me")
    public AuthResponse me(HttpServletRequest request) {
        return RestMapper.toResponse(authUseCase.me(CurrentUserContext.token(request)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authUseCase.logout(CurrentUserContext.token(request));
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

    private AuthResponse toCookieAuthResponse(
            com.euprocuro.api.application.view.AuthenticatedSessionView session,
            HttpServletResponse response
    ) {
        authCookieManager.writeSessionCookie(response, session.getToken(), session.getExpiresAt());

        return AuthResponse.builder()
                .token(exposeSessionToken ? session.getToken() : null)
                .expiresAt(session.getExpiresAt())
                .user(RestMapper.toResponse(session.getUser()))
                .build();
    }
}

package com.euprocuro.api.application.usecase;

import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.GoogleLoginCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.RegisterUserCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.domain.model.UserProfile;

public interface AuthUseCase {
    RegistrationView register(RegisterUserCommand command);

    AuthenticatedSessionView login(LoginCommand command);

    AuthenticatedSessionView loginWithGoogle(GoogleLoginCommand command);

    UserProfile meByUserId(String userId);

    AuthenticatedSessionView requireAuthenticatedSession(String token);

    void logout(String token);

    void logoutIfPresent(String token);

    void deleteCurrentUser(String userId);

    PasswordResetRequestView forgotPassword(ForgotPasswordCommand command);

    void resetPassword(ResetPasswordCommand command);

    void verifyEmail(String token);

    UserProfile requireAuthenticatedUser(String token);
}

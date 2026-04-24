package com.euprocuro.api.application.usecase;

import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.RegisterUserCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.domain.model.UserProfile;

public interface AuthUseCase {
    AuthenticatedSessionView register(RegisterUserCommand command);

    AuthenticatedSessionView login(LoginCommand command);

    AuthenticatedSessionView me(String token);

    void logout(String token);

    PasswordResetRequestView forgotPassword(ForgotPasswordCommand command);

    void resetPassword(ResetPasswordCommand command);

    UserProfile requireAuthenticatedUser(String token);
}

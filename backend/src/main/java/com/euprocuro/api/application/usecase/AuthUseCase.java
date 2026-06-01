package com.euprocuro.api.application.usecase;

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
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.application.view.SocialAuthView;
import com.euprocuro.api.domain.model.UserProfile;

public interface AuthUseCase {
    RegistrationView startRegistration(StartRegistrationCommand command);

    AuthenticatedSessionView confirmRegistration(ConfirmRegistrationCommand command);

    void startPhoneVerification(String userId, StartPhoneVerificationCommand command);

    UserProfile confirmPhoneVerification(String userId, ConfirmPhoneVerificationCommand command);

    AuthenticatedSessionView login(LoginCommand command);

    SocialAuthView loginWithGoogle(GoogleLoginCommand command);

    SocialAuthView loginWithFacebook(FacebookLoginCommand command);

    void startSocialPhoneVerification(StartSocialPhoneVerificationCommand command);

    AuthenticatedSessionView confirmSocialPhoneVerification(ConfirmSocialPhoneVerificationCommand command);

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

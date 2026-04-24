package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResetPasswordCommand {
    String token;
    String newPassword;
    String confirmPassword;
}

package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConfirmSocialPhoneVerificationCommand {
    String socialToken;
    String phone;
    String code;
    String ipAddress;
}

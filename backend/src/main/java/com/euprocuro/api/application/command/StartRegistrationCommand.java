package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StartRegistrationCommand {
    String name;
    String email;
    String password;
    String phone;
    String ipAddress;
    boolean termsAccepted;
    String termsVersion;
}

package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterUserCommand {
    String name;
    String email;
    String documentNumber;
    String password;
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
    String ipAddress;
    boolean termsAccepted;
    String termsVersion;
}

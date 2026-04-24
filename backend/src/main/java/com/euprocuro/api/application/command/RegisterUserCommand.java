package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterUserCommand {
    String name;
    String email;
    String password;
    String city;
    String state;
    String bio;
}

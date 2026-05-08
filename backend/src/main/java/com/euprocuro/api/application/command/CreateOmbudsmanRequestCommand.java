package com.euprocuro.api.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOmbudsmanRequestCommand {
    private String name;
    private String email;
    private String type;
    private String subject;
    private String message;
    private String relatedEntityType;
    private String relatedEntityId;
    private boolean truthDeclarationAccepted;
}

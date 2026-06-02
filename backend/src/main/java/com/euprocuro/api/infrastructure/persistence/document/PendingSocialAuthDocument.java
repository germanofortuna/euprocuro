package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("pending_social_auths")
public class PendingSocialAuthDocument {
    @Id
    private String id;
    @Indexed(unique = true)
    private String token;
    private String provider;
    private String subject;
    private String email;
    private String name;
    private String existingUserId;
    private String ipAddress;
    private Instant createdAt;
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;
}

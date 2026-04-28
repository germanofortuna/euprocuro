package com.euprocuro.api.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.euprocuro.api.domain.gateway.InterestGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingExpirationService {

    private final InterestGateway interestGateway;

    @Value("${application.listings.expiration-days:30}")
    private long listingExpirationDays = 30;

    @Value("${application.listings.cleanup.enabled:true}")
    private boolean cleanupEnabled = true;

    @Scheduled(fixedDelayString = "${application.listings.cleanup.fixed-delay-ms:3600000}")
    public void deleteExpiredListings() {
        if (!cleanupEnabled) {
            return;
        }

        Instant now = Instant.now();
        Instant legacyCutoff = now.minus(Math.max(1, listingExpirationDays), ChronoUnit.DAYS);
        interestGateway.deleteExpired(now, legacyCutoff);
    }
}

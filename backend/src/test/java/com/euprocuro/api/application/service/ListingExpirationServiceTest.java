package com.euprocuro.api.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.domain.gateway.InterestGateway;
@ExtendWith(MockitoExtension.class)
class ListingExpirationServiceTest {

    @Mock
    private InterestGateway interestGateway;

    @InjectMocks
    private ListingExpirationService listingExpirationService;

    @Test
    void deleteExpiredListingsShouldDelegateCleanupWhenEnabled() {
        ReflectionTestUtils.setField(listingExpirationService, "listingExpirationDays", 30L);
        ReflectionTestUtils.setField(listingExpirationService, "cleanupEnabled", true);

        listingExpirationService.deleteExpiredListings();

        verify(interestGateway).deleteExpired(any(Instant.class), any(Instant.class));
    }

    @Test
    void deleteExpiredListingsShouldSkipCleanupWhenDisabled() {
        ReflectionTestUtils.setField(listingExpirationService, "cleanupEnabled", false);

        listingExpirationService.deleteExpiredListings();

        verify(interestGateway, never()).deleteExpired(any(Instant.class), any(Instant.class));
    }
}

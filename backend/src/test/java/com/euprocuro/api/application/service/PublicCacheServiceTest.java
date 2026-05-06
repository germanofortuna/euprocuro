package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PublicCacheServiceTest {

    private PublicCacheService publicCacheService;

    @BeforeEach
    void setUp() {
        publicCacheService = new PublicCacheService();
        ReflectionTestUtils.setField(publicCacheService, "enabled", true);
        ReflectionTestUtils.setField(publicCacheService, "maxEntries", 100);
    }

    @Test
    void getOrLoadShouldReuseCachedValueUntilInvalidated() {
        AtomicInteger loads = new AtomicInteger();

        String first = publicCacheService.getOrLoad(
                PublicCacheService.CONTENT,
                "home",
                60,
                () -> "value-" + loads.incrementAndGet()
        );
        String second = publicCacheService.getOrLoad(
                PublicCacheService.CONTENT,
                "home",
                60,
                () -> "value-" + loads.incrementAndGet()
        );

        assertThat(first).isEqualTo("value-1");
        assertThat(second).isEqualTo("value-1");
        assertThat(loads).hasValue(1);

        publicCacheService.invalidate(PublicCacheService.CONTENT);
        String third = publicCacheService.getOrLoad(
                PublicCacheService.CONTENT,
                "home",
                60,
                () -> "value-" + loads.incrementAndGet()
        );

        assertThat(third).isEqualTo("value-2");
        assertThat(loads).hasValue(2);
    }
}

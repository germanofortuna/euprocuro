package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

    @Test
    void getOrLoadShouldBypassCacheWhenDisabledOrTtlIsNotPositive() {
        AtomicInteger loads = new AtomicInteger();

        ReflectionTestUtils.setField(publicCacheService, "enabled", false);
        assertThat(publicCacheService.getOrLoad(PublicCacheService.CONTENT, "home", 60, loads::incrementAndGet))
                .isEqualTo(1);
        assertThat(publicCacheService.getOrLoad(PublicCacheService.CONTENT, "home", 60, loads::incrementAndGet))
                .isEqualTo(2);

        ReflectionTestUtils.setField(publicCacheService, "enabled", true);
        assertThat(publicCacheService.getOrLoad(PublicCacheService.CONTENT, "home", 0, loads::incrementAndGet))
                .isEqualTo(3);
    }

    @Test
    void getOrLoadShouldNotCacheNullValues() {
        AtomicInteger loads = new AtomicInteger();

        String first = publicCacheService.getOrLoad(PublicCacheService.CONTENT, "empty", 60, () -> {
            loads.incrementAndGet();
            return null;
        });
        String second = publicCacheService.getOrLoad(PublicCacheService.CONTENT, "empty", 60, () -> {
            loads.incrementAndGet();
            return "loaded";
        });

        assertThat(first).isNull();
        assertThat(second).isEqualTo("loaded");
        assertThat(loads).hasValue(2);
    }

    @Test
    void invalidateShouldNormalizeUnknownScopeToAllAndExposeSnapshot() {
        publicCacheService.getOrLoad(PublicCacheService.CONTENT, "home", 60, () -> "home");
        publicCacheService.getOrLoad(PublicCacheService.CATALOG, "categories", 60, () -> List.of("AUTO"));

        var snapshot = publicCacheService.snapshot();
        assertThat(snapshot.getEntries()).isEqualTo(2);
        assertThat(snapshot.getProvider()).isEqualTo("LOCAL_MEMORY");
        assertThat(snapshot.isEnabled()).isTrue();

        var invalidation = publicCacheService.invalidate(" invalid ");

        assertThat(invalidation.getScope()).isEqualTo(PublicCacheService.ALL);
        assertThat(invalidation.getEntries()).isZero();
        assertThat(invalidation.getInvalidatedAt()).isNotNull();
    }

    @Test
    void getOrLoadShouldPruneOldEntriesWhenLimitIsReached() {
        ReflectionTestUtils.setField(publicCacheService, "maxEntries", 2);

        publicCacheService.getOrLoad(PublicCacheService.CONTENT, "one", 60, () -> "1");
        publicCacheService.getOrLoad(PublicCacheService.CONTENT, "two", 60, () -> "2");
        publicCacheService.getOrLoad(PublicCacheService.CONTENT, "three", 60, () -> "3");

        assertThat(publicCacheService.snapshot().getEntries()).isLessThanOrEqualTo(3);
    }
}

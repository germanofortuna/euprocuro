package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.view.CacheInvalidationView;

@Service
public class PublicCacheService {

    public static final String CONTENT = "content";
    public static final String CATALOG = "catalog";
    public static final String MARKETPLACE = "marketplace";
    public static final String ADDRESS = "address";
    public static final String ALL = "all";

    private static final String PROVIDER = "LOCAL_MEMORY";

    private final ConcurrentMap<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> versions = new ConcurrentHashMap<>();

    @Value("${application.cache.public.enabled:true}")
    private boolean enabled = true;

    @Value("${application.cache.public.max-entries:2000}")
    private int maxEntries = 2000;

    public <T> T getOrLoad(String namespace, String rawKey, long ttlSeconds, Supplier<T> loader) {
        if (!enabled || ttlSeconds <= 0) {
            return loader.get();
        }

        String cacheKey = cacheKey(namespace, rawKey);
        Instant now = Instant.now();
        CacheEntry current = entries.get(cacheKey);
        if (current != null && current.isAlive(now)) {
            return cast(current.getValue());
        }

        T loaded = loader.get();
        if (loaded == null) {
            entries.remove(cacheKey);
            return null;
        }

        pruneIfNeeded(now);
        entries.put(cacheKey, CacheEntry.of(loaded, now.plusSeconds(ttlSeconds)));
        return loaded;
    }

    public CacheInvalidationView snapshot() {
        pruneExpired(Instant.now());
        return buildView(ALL, null);
    }

    public CacheInvalidationView invalidate(String namespace) {
        String resolvedNamespace = normalizeNamespace(namespace);
        if (ALL.equals(resolvedNamespace)) {
            return invalidateAll();
        }

        versions.computeIfAbsent(resolvedNamespace, key -> new AtomicLong(1)).incrementAndGet();
        String prefix = resolvedNamespace + ":";
        entries.keySet().removeIf(key -> key.startsWith(prefix));
        return buildView(resolvedNamespace, Instant.now());
    }

    public CacheInvalidationView invalidateAll() {
        versions.values().forEach(AtomicLong::incrementAndGet);
        entries.clear();
        return buildView(ALL, Instant.now());
    }

    private String cacheKey(String namespace, String rawKey) {
        String resolvedNamespace = normalizeNamespace(namespace);
        long version = versions.computeIfAbsent(resolvedNamespace, key -> new AtomicLong(1)).get();
        return resolvedNamespace + ":v" + version + ":" + normalizeKey(rawKey);
    }

    private String normalizeNamespace(String namespace) {
        if (!StringUtils.hasText(namespace)) {
            return ALL;
        }
        String normalized = namespace.trim().toLowerCase(Locale.ROOT);
        if (!CONTENT.equals(normalized)
                && !CATALOG.equals(normalized)
                && !MARKETPLACE.equals(normalized)
                && !ADDRESS.equals(normalized)
                && !ALL.equals(normalized)) {
            return ALL;
        }
        return normalized;
    }

    private String normalizeKey(String rawKey) {
        return StringUtils.hasText(rawKey) ? rawKey.trim().toLowerCase(Locale.ROOT) : "default";
    }

    private void pruneIfNeeded(Instant now) {
        if (entries.size() < Math.max(1, maxEntries)) {
            return;
        }
        pruneExpired(now);
        if (entries.size() < Math.max(1, maxEntries)) {
            return;
        }

        int removeCount = Math.max(1, entries.size() / 10);
        entries.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getExpiresAt()))
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
                .forEach(entries::remove);
    }

    private void pruneExpired(Instant now) {
        entries.entrySet().removeIf(entry -> !entry.getValue().isAlive(now));
    }

    private CacheInvalidationView buildView(String scope, Instant invalidatedAt) {
        Map<String, Long> snapshot = versions.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        return CacheInvalidationView.builder()
                .scope(scope)
                .enabled(enabled)
                .provider(PROVIDER)
                .entries(entries.size())
                .versions(snapshot)
                .invalidatedAt(invalidatedAt)
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        return (T) value;
    }

    private static class CacheEntry {
        private final Object value;
        private final Instant expiresAt;

        private CacheEntry(Object value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        private static CacheEntry of(Object value, Instant expiresAt) {
            return new CacheEntry(value, expiresAt);
        }

        private boolean isAlive(Instant now) {
            return expiresAt != null && expiresAt.isAfter(now);
        }

        private Object getValue() {
            return value;
        }

        private Instant getExpiresAt() {
            return expiresAt;
        }
    }
}

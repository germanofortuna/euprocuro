package com.euprocuro.api.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.SaveContentEntryCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.AdminContentUseCase;
import com.euprocuro.api.application.usecase.ContentUseCase;
import com.euprocuro.api.application.view.AdminContentCatalogView;
import com.euprocuro.api.application.view.ContentEntryView;
import com.euprocuro.api.application.view.ContentRevisionView;
import com.euprocuro.api.application.view.PublicContentCatalogView;
import com.euprocuro.api.domain.gateway.ContentEntryGateway;
import com.euprocuro.api.domain.gateway.ContentRevisionGateway;
import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentEntryType;
import com.euprocuro.api.domain.model.ContentRevision;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentService implements ContentUseCase, AdminContentUseCase {

    private static final String DEFAULT_LOCALE = "pt-BR";
    private static final int MAX_KEY_LENGTH = 160;
    private static final int MAX_VALUE_LENGTH = 120_000;

    private final AdminAccessService adminAccessService;
    private final ContentEntryGateway contentEntryGateway;
    private final ContentRevisionGateway contentRevisionGateway;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean defaultsSeeded = new AtomicBoolean(false);

    @PostConstruct
    public void seedDefaultsOnStartup() {
        ensureDefaultContentSeeded();
    }

    @Override
    public PublicContentCatalogView getPublishedContent(String locale, List<String> keys) {
        ensureDefaultContentSeeded();
        String resolvedLocale = resolveLocale(locale);
        List<ContentEntry> entries = keys == null || keys.isEmpty()
                ? contentEntryGateway.findByStatusAndLocale(ContentEntryStatus.PUBLISHED, resolvedLocale)
                : contentEntryGateway.findByStatusAndLocaleAndKeyIn(
                        ContentEntryStatus.PUBLISHED,
                        resolvedLocale,
                        sanitizeKeys(keys)
                );
        List<ContentEntryView> publishedEntries = entries.stream()
                .filter(entry -> entry.getType() != ContentEntryType.CATALOG)
                .filter(entry -> StringUtils.hasText(entry.getPublishedValue()))
                .map(this::toPublicView)
                .collect(Collectors.toList());

        return PublicContentCatalogView.builder()
                .locale(resolvedLocale)
                .version(buildVersion(publishedEntries))
                .entries(publishedEntries)
                .build();
    }

    @Override
    public AdminContentCatalogView getContentEntries(String currentUserId) {
        ensureDefaultContentSeeded();
        adminAccessService.requireAdmin(currentUserId);
        return AdminContentCatalogView.builder()
                .entries(contentEntryGateway.findAll()
                        .stream()
                        .map(this::toAdminView)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public ContentEntryView saveDraft(String currentUserId, String entryId, SaveContentEntryCommand command) {
        ensureDefaultContentSeeded();
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        validateSaveCommand(command);

        String resolvedLocale = resolveLocale(command.getLocale());
        Instant now = Instant.now();
        ContentEntry existing = resolveExistingEntry(entryId, command.getKey(), resolvedLocale).orElse(null);
        ContentEntryStatus nextStatus = nextDraftStatus(existing);

        ContentEntry entry = ContentEntry.builder()
                .id(existing == null ? null : existing.getId())
                .key(normalizeKey(command.getKey()))
                .type(Optional.ofNullable(command.getType()).orElse(ContentEntryType.TEXT))
                .locale(resolvedLocale)
                .status(nextStatus)
                .version(existing == null ? 0 : existing.getVersion())
                .draftValue(command.getDraftValue().trim())
                .publishedValue(existing == null ? null : existing.getPublishedValue())
                .description(trimToNull(command.getDescription()))
                .screen(resolveScreen(command.getScreen(), command.getKey()))
                .legalSlug(trimToNull(command.getLegalSlug()))
                .requiresUserAcceptance(command.isRequiresUserAcceptance())
                .effectiveFrom(command.getEffectiveFrom())
                .createdAt(existing == null ? now : existing.getCreatedAt())
                .updatedAt(now)
                .publishedAt(existing == null ? null : existing.getPublishedAt())
                .updatedBy(admin.getId())
                .publishedBy(existing == null ? null : existing.getPublishedBy())
                .build();

        return toAdminView(contentEntryGateway.save(entry));
    }

    @Override
    public ContentEntryView publish(String currentUserId, String entryId) {
        ensureDefaultContentSeeded();
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        ContentEntry existing = contentEntryGateway.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Conteudo nao encontrado."));

        if (!StringUtils.hasText(existing.getDraftValue())) {
            throw new BusinessException("Informe um texto antes de publicar.");
        }

        Instant now = Instant.now();
        ContentEntry published = contentEntryGateway.save(existing.toBuilder()
                .status(ContentEntryStatus.PUBLISHED)
                .version(existing.getVersion() + 1)
                .publishedValue(existing.getDraftValue())
                .publishedAt(now)
                .updatedAt(now)
                .publishedBy(admin.getId())
                .updatedBy(admin.getId())
                .build());

        contentRevisionGateway.save(ContentRevision.builder()
                .contentEntryId(published.getId())
                .key(published.getKey())
                .locale(published.getLocale())
                .version(published.getVersion())
                .snapshotValue(published.getPublishedValue())
                .publishedBy(admin.getId())
                .publishedAt(now)
                .build());

        return toAdminView(published);
    }

    @Override
    public ContentEntryView archive(String currentUserId, String entryId) {
        ensureDefaultContentSeeded();
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        ContentEntry existing = contentEntryGateway.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Conteudo nao encontrado."));
        Instant now = Instant.now();

        return toAdminView(contentEntryGateway.save(existing.toBuilder()
                .status(ContentEntryStatus.ARCHIVED)
                .updatedAt(now)
                .updatedBy(admin.getId())
                .build()));
    }

    @Override
    public List<ContentRevisionView> getRevisions(String currentUserId, String entryId) {
        ensureDefaultContentSeeded();
        adminAccessService.requireAdmin(currentUserId);
        ContentEntry existing = contentEntryGateway.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Conteudo nao encontrado."));
        return contentRevisionGateway.findByContentEntryId(existing.getId())
                .stream()
                .map(this::toRevisionView)
                .collect(Collectors.toList());
    }

    private void ensureDefaultContentSeeded() {
        if (!defaultsSeeded.compareAndSet(false, true)) {
            return;
        }

        ClassPathResource resource = new ClassPathResource("content/default-content.json");
        if (!resource.exists()) {
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            DefaultContentCatalog catalog = objectMapper.readValue(inputStream, DefaultContentCatalog.class);
            seedTextEntries(Optional.ofNullable(catalog.getEntries()).orElse(Map.of()));
            seedLegalEntries(Optional.ofNullable(catalog.getLegalPages()).orElse(Map.of()));
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar o catalogo padrao de conteudo.", exception);
        }
    }

    private void seedTextEntries(Map<String, String> entries) {
        entries.forEach((key, value) -> seedEntry(
                normalizeKey(key),
                ContentEntryType.TEXT,
                value,
                null,
                false
        ));
    }

    private void seedLegalEntries(Map<String, JsonNode> legalPages) {
        legalPages.forEach((slug, page) -> {
            try {
                String key = "legal.page." + normalizeKey(slug);
                seedEntry(key, ContentEntryType.LEGAL_DOCUMENT, objectMapper.writeValueAsString(page), slug, "termos-de-uso".equals(slug));
            } catch (IOException exception) {
                throw new IllegalStateException("Nao foi possivel serializar documento legal padrao.", exception);
            }
        });
    }

    private void seedEntry(
            String key,
            ContentEntryType type,
            String value,
            String legalSlug,
            boolean requiresUserAcceptance
    ) {
        if (!StringUtils.hasText(value) || contentEntryGateway.findByKeyAndLocale(key, DEFAULT_LOCALE).isPresent()) {
            return;
        }

        Instant now = Instant.now();
        ContentEntry saved = contentEntryGateway.save(ContentEntry.builder()
                .key(key)
                .type(type)
                .locale(DEFAULT_LOCALE)
                .status(ContentEntryStatus.PUBLISHED)
                .version(1)
                .draftValue(value)
                .publishedValue(value)
                .description("Conteudo inicial da plataforma.")
                .screen(resolveScreen(null, key))
                .legalSlug(legalSlug)
                .requiresUserAcceptance(requiresUserAcceptance)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .build());

        contentRevisionGateway.save(ContentRevision.builder()
                .contentEntryId(saved.getId())
                .key(saved.getKey())
                .locale(saved.getLocale())
                .version(saved.getVersion())
                .snapshotValue(saved.getPublishedValue())
                .publishedAt(now)
                .build());
    }

    private Optional<ContentEntry> resolveExistingEntry(String entryId, String key, String locale) {
        if (StringUtils.hasText(entryId)) {
            return contentEntryGateway.findById(entryId);
        }
        return contentEntryGateway.findByKeyAndLocale(normalizeKey(key), locale);
    }

    private void validateSaveCommand(SaveContentEntryCommand command) {
        if (command == null) {
            throw new BusinessException("Informe o conteudo para salvar.");
        }
        String normalizedKey = normalizeKey(command.getKey());
        if (!StringUtils.hasText(normalizedKey) || normalizedKey.length() > MAX_KEY_LENGTH) {
            throw new BusinessException("Informe uma chave valida para o conteudo.");
        }
        if (!normalizedKey.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new BusinessException("Use apenas letras minusculas, numeros, ponto, hifen ou underline na chave.");
        }
        if (!StringUtils.hasText(command.getDraftValue())) {
            throw new BusinessException("Informe o texto do conteudo.");
        }
        if (command.getDraftValue().length() > MAX_VALUE_LENGTH) {
            throw new BusinessException("O conteudo informado esta acima do limite permitido.");
        }
    }

    private ContentEntryStatus nextDraftStatus(ContentEntry existing) {
        if (existing == null || existing.getStatus() == ContentEntryStatus.ARCHIVED) {
            return ContentEntryStatus.DRAFT;
        }
        return existing.getStatus();
    }

    private Collection<String> sanitizeKeys(List<String> keys) {
        return keys.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeKey)
                .collect(Collectors.toSet());
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveLocale(String locale) {
        return StringUtils.hasText(locale) ? locale.trim() : DEFAULT_LOCALE;
    }

    private String resolveScreen(String screen, String key) {
        if (StringUtils.hasText(screen)) {
            return screen.trim();
        }
        String normalizedKey = normalizeKey(key);
        int separatorIndex = normalizedKey.indexOf('.');
        return separatorIndex > 0 ? normalizedKey.substring(0, separatorIndex) : "global";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildVersion(List<ContentEntryView> entries) {
        return Integer.toHexString(entries.stream()
                .map(entry -> entry.getKey() + ":" + entry.getVersion())
                .collect(Collectors.joining("|"))
                .hashCode());
    }

    private ContentEntryView toPublicView(ContentEntry entry) {
        return ContentEntryView.builder()
                .key(entry.getKey())
                .type(entry.getType())
                .locale(entry.getLocale())
                .version(entry.getVersion())
                .publicValue(entry.getPublishedValue())
                .legalSlug(entry.getLegalSlug())
                .requiresUserAcceptance(entry.isRequiresUserAcceptance())
                .effectiveFrom(entry.getEffectiveFrom())
                .publishedAt(entry.getPublishedAt())
                .build();
    }

    private ContentEntryView toAdminView(ContentEntry entry) {
        return ContentEntryView.builder()
                .id(entry.getId())
                .key(entry.getKey())
                .type(entry.getType())
                .locale(entry.getLocale())
                .status(entry.getStatus())
                .version(entry.getVersion())
                .draftValue(entry.getDraftValue())
                .publishedValue(entry.getPublishedValue())
                .description(entry.getDescription())
                .screen(entry.getScreen())
                .legalSlug(entry.getLegalSlug())
                .requiresUserAcceptance(entry.isRequiresUserAcceptance())
                .effectiveFrom(entry.getEffectiveFrom())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .publishedAt(entry.getPublishedAt())
                .build();
    }

    private ContentRevisionView toRevisionView(ContentRevision revision) {
        return ContentRevisionView.builder()
                .id(revision.getId())
                .contentEntryId(revision.getContentEntryId())
                .key(revision.getKey())
                .locale(revision.getLocale())
                .version(revision.getVersion())
                .snapshotValue(revision.getSnapshotValue())
                .publishedAt(revision.getPublishedAt())
                .build();
    }

    @Data
    private static class DefaultContentCatalog {
        private String version;
        private Map<String, String> entries = new LinkedHashMap<>();
        private Map<String, JsonNode> legalPages = new LinkedHashMap<>();
    }
}

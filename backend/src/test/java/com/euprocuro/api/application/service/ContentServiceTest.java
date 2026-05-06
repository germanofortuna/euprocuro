package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.SaveContentEntryCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.view.AdminContentCatalogView;
import com.euprocuro.api.application.view.ContentEntryView;
import com.euprocuro.api.application.view.PublicContentCatalogView;
import com.euprocuro.api.domain.gateway.ContentEntryGateway;
import com.euprocuro.api.domain.gateway.ContentRevisionGateway;
import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentEntryType;
import com.euprocuro.api.domain.model.ContentRevision;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private AdminAccessService adminAccessService;
    @Mock
    private ContentEntryGateway contentEntryGateway;
    @Mock
    private ContentRevisionGateway contentRevisionGateway;
    @Mock
    private PublicCacheService publicCacheService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContentService contentService;

    private UserProfile admin;

    @BeforeEach
    void setUp() {
        admin = UserProfile.builder()
                .id("admin-1")
                .name("Admin User")
                .email("admin@test.com")
                .build();
        
        // Mockar o método ensureDefaultContentSeeded para não tentar ler ficheiro
        ReflectionTestUtils.setField(contentService, "defaultsSeeded", new java.util.concurrent.atomic.AtomicBoolean(true));
    }

    @Test
    void getPublishedContentShouldReturnEntriesForLocaleWithoutKeys() {
        ContentEntry entry = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .type(ContentEntryType.TEXT)
                .locale("pt-BR")
                .status(ContentEntryStatus.PUBLISHED)
                .version(1)
                .publishedValue("Bem vindo")
                .build();

        when(contentEntryGateway.findByStatusAndLocale(ContentEntryStatus.PUBLISHED, "pt-BR"))
                .thenReturn(List.of(entry));
        when(publicCacheService.getOrLoad(eq("content"), any(), eq(300L), any()))
                .thenAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get());

        PublicContentCatalogView result = contentService.getPublishedContent("pt-BR", null);

        assertThat(result).isNotNull();
        assertThat(result.getLocale()).isEqualTo("pt-BR");
        assertThat(result.getEntries()).hasSize(1);
        assertThat(result.getEntries().get(0).getKey()).isEqualTo("home.title");
    }

    @Test
    void getPublishedContentShouldReturnEntriesForSpecificKeys() {
        ContentEntry entry = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .type(ContentEntryType.TEXT)
                .locale("pt-BR")
                .status(ContentEntryStatus.PUBLISHED)
                .version(1)
                .publishedValue("Bem vindo")
                .build();

        when(contentEntryGateway.findByStatusAndLocaleAndKeyIn(
                ContentEntryStatus.PUBLISHED,
                "pt-BR",
                List.of("home.title")))
                .thenReturn(List.of(entry));
        when(publicCacheService.getOrLoad(eq("content"), any(), eq(300L), any()))
                .thenAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get());

        PublicContentCatalogView result = contentService.getPublishedContent("pt-BR", List.of("home.title"));

        assertThat(result).isNotNull();
        assertThat(result.getEntries()).hasSize(1);
    }

    @Test
    void getPublishedContentShouldUseDefaultLocaleWhenNotProvided() {
        when(contentEntryGateway.findByStatusAndLocale(ContentEntryStatus.PUBLISHED, "pt-BR"))
                .thenReturn(List.of());
        when(publicCacheService.getOrLoad(eq("content"), any(), eq(300L), any()))
                .thenAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get());

        PublicContentCatalogView result = contentService.getPublishedContent(null, null);

        assertThat(result.getLocale()).isEqualTo("pt-BR");
    }

    @Test
    void getPublishedContentShouldFilterOutCatalogEntries() {
        ContentEntry catalogEntry = ContentEntry.builder()
                .id("entry-1")
                .key("catalog.categories")
                .type(ContentEntryType.CATALOG)
                .locale("pt-BR")
                .status(ContentEntryStatus.PUBLISHED)
                .publishedValue("[]")
                .build();

        when(contentEntryGateway.findByStatusAndLocale(ContentEntryStatus.PUBLISHED, "pt-BR"))
                .thenReturn(List.of(catalogEntry));
        when(publicCacheService.getOrLoad(eq("content"), any(), eq(300L), any()))
                .thenAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get());

        PublicContentCatalogView result = contentService.getPublishedContent("pt-BR", null);

        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    void getContentEntriesShouldRequireAdminAccess() {
        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> contentService.getContentEntries("user-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getContentEntriesShouldReturnAllEntries() {
        ContentEntry entry1 = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .status(ContentEntryStatus.PUBLISHED)
                .build();
        ContentEntry entry2 = ContentEntry.builder()
                .id("entry-2")
                .key("home.subtitle")
                .status(ContentEntryStatus.DRAFT)
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findAll()).thenReturn(List.of(entry1, entry2));

        AdminContentCatalogView result = contentService.getContentEntries("admin-1");

        assertThat(result.getEntries()).hasSize(2);
        assertThat(result.getEntries()).extracting(ContentEntryView::getKey)
                .containsExactlyInAnyOrder("home.title", "home.subtitle");
    }

    @Test
    void saveDraftShouldRequireAdminAccess() {
        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("home.title")
                .draftValue("Titulo")
                .build();

        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> contentService.saveDraft("user-1", null, command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void saveDraftShouldRejectNullCommand() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> contentService.saveDraft("admin-1", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe o conteudo");
    }

    @Test
    void saveDraftShouldRejectEmptyKey() {
        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("")
                .draftValue("Titulo")
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> contentService.saveDraft("admin-1", null, command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chave valida");
    }

    @Test
    void saveDraftShouldRejectEmptyDraftValue() {
        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("home.title")
                .draftValue("")
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> contentService.saveDraft("admin-1", null, command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe o texto");
    }

    @Test
    void saveDraftShouldRejectInvalidKeyFormat() {
        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("HOME@TITLE")
                .draftValue("Titulo")
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> contentService.saveDraft("admin-1", null, command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("minusculas");
    }

    @Test
    void saveDraftShouldCreateNewDraftEntry() {
        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("home.title")
                .draftValue("Bem vindo")
                .type(ContentEntryType.TEXT)
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findByKeyAndLocale("home.title", "pt-BR")).thenReturn(Optional.empty());
        when(contentEntryGateway.save(any(ContentEntry.class))).thenAnswer(invocation -> {
            ContentEntry entry = invocation.getArgument(0);
            entry.setId("new-entry");
            return entry;
        });

        ContentEntryView result = contentService.saveDraft("admin-1", null, command);

        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo("home.title");
        assertThat(result.getDraftValue()).isEqualTo("Bem vindo");
        assertThat(result.getStatus()).isEqualTo(ContentEntryStatus.DRAFT);

        ArgumentCaptor<ContentEntry> entryCaptor = ArgumentCaptor.forClass(ContentEntry.class);
        verify(contentEntryGateway).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getUpdatedBy()).isEqualTo("admin-1");
    }

    @Test
    void saveDraftShouldUpdateExistingDraftEntry() {
        ContentEntry existing = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .status(ContentEntryStatus.DRAFT)
                .version(1)
                .draftValue("Old value")
                .publishedValue("Published")
                .locale("pt-BR")
                .build();

        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("home.title")
                .draftValue("New value")
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findByKeyAndLocale("home.title", "pt-BR")).thenReturn(Optional.of(existing));
        when(contentEntryGateway.save(any(ContentEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContentEntryView result = contentService.saveDraft("admin-1", null, command);

        assertThat(result.getDraftValue()).isEqualTo("New value");
        assertThat(result.getPublishedValue()).isEqualTo("Published");
    }

    @Test
    void publishShouldRequireAdminAccess() {
        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> contentService.publish("user-1", "entry-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void publishShouldRejectMissingEntry() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.publish("admin-1", "missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    void publishShouldRejectEmptyDraftValue() {
        ContentEntry entry = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .draftValue("")
                .status(ContentEntryStatus.DRAFT)
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("entry-1")).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> contentService.publish("admin-1", "entry-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe um texto");
    }

    @Test
    void publishShouldPublishDraftAndCreateRevision() {
        ContentEntry draft = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .draftValue("Bem vindo")
                .status(ContentEntryStatus.DRAFT)
                .version(1)
                .locale("pt-BR")
                .publishedValue(null)
                .createdAt(Instant.now())
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("entry-1")).thenReturn(Optional.of(draft));
        when(contentEntryGateway.save(any(ContentEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentRevisionGateway.save(any(ContentRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContentEntryView result = contentService.publish("admin-1", "entry-1");

        assertThat(result.getStatus()).isEqualTo(ContentEntryStatus.PUBLISHED);
        assertThat(result.getPublishedValue()).isEqualTo("Bem vindo");
        assertThat(result.getVersion()).isEqualTo(2);

        verify(contentEntryGateway).save(any(ContentEntry.class));
        verify(contentRevisionGateway).save(any(ContentRevision.class));
        verify(publicCacheService).invalidate("content");
    }

    @Test
    void archiveShouldRequireAdminAccess() {
        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> contentService.archive("user-1", "entry-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void archiveShouldRejectMissingEntry() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.archive("admin-1", "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archiveShouldArchiveEntryAndInvalidateCache() {
        ContentEntry entry = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .status(ContentEntryStatus.DRAFT)
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("entry-1")).thenReturn(Optional.of(entry));
        when(contentEntryGateway.save(any(ContentEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContentEntryView result = contentService.archive("admin-1", "entry-1");

        assertThat(result.getStatus()).isEqualTo(ContentEntryStatus.ARCHIVED);
        verify(publicCacheService).invalidate("content");
    }

    @Test
    void getRevisionsShouldRequireAdminAccess() {
        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> contentService.getRevisions("user-1", "entry-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getRevisionsShouldRejectMissingEntry() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.getRevisions("admin-1", "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRevisionsShouldReturnRevisionHistory() {
        ContentEntry entry = ContentEntry.builder()
                .id("entry-1")
                .key("home.title")
                .build();

        ContentRevision revision1 = ContentRevision.builder()
                .id("rev-1")
                .contentEntryId("entry-1")
                .key("home.title")
                .version(1)
                .snapshotValue("Version 1")
                .publishedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        ContentRevision revision2 = ContentRevision.builder()
                .id("rev-2")
                .contentEntryId("entry-1")
                .key("home.title")
                .version(2)
                .snapshotValue("Version 2")
                .publishedAt(Instant.now())
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);
        when(contentEntryGateway.findById("entry-1")).thenReturn(Optional.of(entry));
        when(contentRevisionGateway.findByContentEntryId("entry-1"))
                .thenReturn(List.of(revision1, revision2));

        var result = contentService.getRevisions("admin-1", "entry-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(1);
        assertThat(result.get(1).getVersion()).isEqualTo(2);
    }

    @Test
    void saveDraftShouldRejectValuesTooLarge() {
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 121_000; i++) {
            largeValue.append("x");
        }

        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key("home.title")
                .draftValue(largeValue.toString())
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> contentService.saveDraft("admin-1", null, command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("acima do limite");
    }

    @Test
    void saveDraftShouldRejectKeysTooLarge() {
        StringBuilder largeKey = new StringBuilder();
        for (int i = 0; i < 161; i++) {
            largeKey.append("a");
        }

        SaveContentEntryCommand command = SaveContentEntryCommand.builder()
                .key(largeKey.toString())
                .draftValue("Value")
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> contentService.saveDraft("admin-1", null, command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chave valida");
    }
}





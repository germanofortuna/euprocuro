package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.CatalogCategoryCommand;
import com.euprocuro.api.application.command.CatalogProductCommand;
import com.euprocuro.api.application.command.ModerationSettingsCommand;
import com.euprocuro.api.application.command.MonetizationSettingsCommand;
import com.euprocuro.api.application.command.OperationalFlagsCommand;
import com.euprocuro.api.application.command.SaveOperationalCatalogCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.domain.gateway.ContentEntryGateway;
import com.euprocuro.api.domain.gateway.ContentRevisionGateway;
import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentRevision;
import com.euprocuro.api.domain.model.MonetizationProductType;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class OperationalCatalogServiceIntegrationStyleTest {

    @Mock
    private AdminAccessService adminAccessService;

    private InMemoryContentEntryGateway contentEntryGateway;
    private InMemoryContentRevisionGateway contentRevisionGateway;
    private PublicCacheService publicCacheService;
    private OperationalCatalogService service;

    @BeforeEach
    void setUp() {
        contentEntryGateway = new InMemoryContentEntryGateway();
        contentRevisionGateway = new InMemoryContentRevisionGateway();
        publicCacheService = spy(new PublicCacheService());
        ReflectionTestUtils.setField(publicCacheService, "enabled", true);
        ReflectionTestUtils.setField(publicCacheService, "maxEntries", 100);
        service = new OperationalCatalogService(
                adminAccessService,
                contentEntryGateway,
                contentRevisionGateway,
                new ObjectMapper(),
                publicCacheService
        );
    }

    @Test
    void listActiveCategoriesAndProductsShouldSeedDefaultsWithMonetizationDisabled() {
        var categories = service.listActiveCategories();
        var products = service.listActiveProducts();

        assertThat(categories).extracting("code")
                .containsExactly("AUTOMOVEIS", "IMOVEIS", "SERVICOS", "ELETRONICOS", "INSTRUMENTOS", "OUTROS", "FIGURINHAS");
        assertThat(products).isEmpty();
        assertThat(service.getMonetizationSettings().isCreditPurchasesEnabled()).isFalse();
        assertThat(service.getMonetizationSettings().isBoostPurchasesEnabled()).isFalse();
        assertThat(service.getModerationSettings().isUserBlockListEnabled()).isTrue();
        assertThat(service.getFeatureFlags().isStickersPageEnabled()).isTrue();
        assertThat(service.getOperationalFields().getInitialFreeCredits()).isEqualTo(15);
        assertThat(service.getOperationalFields().getListingRenewalCredits()).isEqualTo(1);
        assertThat(contentEntryGateway.findAll()).hasSize(6);
        assertThat(contentRevisionGateway.revisions).hasSize(6);
    }

    @Test
    void requireActiveCategoryShouldReturnNormalizedCodeAndRejectInactiveOnes() {
        assertThat(service.requireActiveCategory(" automoveis ")).isEqualTo("AUTOMOVEIS");

        assertThatThrownBy(() -> service.requireActiveCategory("nao-existe"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Categoria invalida");
    }

    @Test
    void requireActiveCategoryShouldBackfillStickersWhenExistingCatalogWasSeededBeforeFeature() {
        contentEntryGateway.save(ContentEntry.builder()
                .key("catalog.categories")
                .locale("pt-BR")
                .status(ContentEntryStatus.PUBLISHED)
                .version(1)
                .publishedValue("[{\"code\":\"SERVICOS\",\"label\":\"Servicos\",\"active\":true,\"sortOrder\":10}]")
                .draftValue("[{\"code\":\"SERVICOS\",\"label\":\"Servicos\",\"active\":true,\"sortOrder\":10}]")
                .build());

        assertThat(service.requireActiveCategory("figurinhas")).isEqualTo("FIGURINHAS");
        assertThat(service.listActiveCategories()).extracting("code").contains("SERVICOS", "FIGURINHAS");
        verify(publicCacheService, atLeastOnce()).invalidate(PublicCacheService.CATALOG);
    }

    @Test
    void saveAdminCatalogShouldPersistValidatedCatalogAndInvalidateCache() {
        when(adminAccessService.requireAdmin("admin-1"))
                .thenReturn(UserProfile.builder().id("admin-1").email("admin@test.com").build());

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .monetizationSettings(MonetizationSettingsCommand.builder()
                        .creditPurchasesEnabled(true)
                        .boostPurchasesEnabled(false)
                        .build())
                .moderationSettings(ModerationSettingsCommand.builder()
                        .userBlockListEnabled(false)
                        .build())
                .categories(List.of(
                        CatalogCategoryCommand.builder()
                                .code(" auto ")
                                .label(" Automoveis ")
                                .active(true)
                                .sortOrder(null)
                                .build(),
                        CatalogCategoryCommand.builder()
                                .code("servicos")
                                .label("Servicos")
                                .active(false)
                                .sortOrder(10)
                                .build()))
                .products(List.of(
                        CatalogProductCommand.builder()
                                .code("credits_10")
                                .name(" 10 creditos ")
                                .description(" Pacote ")
                                .type(MonetizationProductType.CREDIT_PACK)
                                .price(new BigDecimal("8.90"))
                                .promotional(true)
                                .originalPrice(new BigDecimal("12.90"))
                                .promotionLabel(" Oferta ")
                                .credits(10)
                                .enabled(true)
                                .build(),
                        CatalogProductCommand.builder()
                                .code("boost_3_days")
                                .name("Boost")
                                .type(MonetizationProductType.BOOST)
                                .price(null)
                                .durationDays(3)
                                .enabled(false)
                                .sortOrder(5)
                                .build()))
                .build();

        var adminCatalog = service.saveAdminCatalog("admin-1", command);

        assertThat(adminCatalog.getCategories()).hasSize(3);
        assertThat(service.listActiveCategories()).extracting("code").containsExactly("AUTO", "FIGURINHAS");
        assertThat(adminCatalog.getProducts()).extracting("code").containsExactly("CREDITS_10", "BOOST_3_DAYS");
        assertThat(adminCatalog.getProducts().get(0).getOriginalPrice()).isEqualByComparingTo("12.90");
        assertThat(adminCatalog.getProducts().get(0).getPromotionLabel()).isEqualTo("Oferta");
        assertThat(service.getMonetizationSettings().isCreditPurchasesEnabled()).isFalse();
        assertThat(service.getMonetizationSettings().isBoostPurchasesEnabled()).isFalse();
        assertThat(service.getModerationSettings().isUserBlockListEnabled()).isTrue();
        verify(publicCacheService, atLeastOnce()).invalidate(PublicCacheService.CATALOG);
    }

    @Test
    void saveOperationalFlagsShouldNotValidateProductsOrCategories() {
        when(adminAccessService.requireAdmin("admin-1"))
                .thenReturn(UserProfile.builder().id("admin-1").email("admin@test.com").build());

        var adminCatalog = service.saveOperationalFlags("admin-1", OperationalFlagsCommand.builder()
                .monetizationSettings(MonetizationSettingsCommand.builder()
                        .creditPurchasesEnabled(true)
                        .boostPurchasesEnabled(true)
                        .build())
                .moderationSettings(ModerationSettingsCommand.builder()
                        .userBlockListEnabled(false)
                        .build())
                .build());

        assertThat(adminCatalog.getCategories()).extracting("code")
                .containsExactly("AUTOMOVEIS", "IMOVEIS", "SERVICOS", "ELETRONICOS", "INSTRUMENTOS", "OUTROS", "FIGURINHAS");
        assertThat(adminCatalog.getProducts()).extracting("code")
                .contains("CREDITS_10", "BOOST_3_DAYS");
        assertThat(service.getMonetizationSettings().isCreditPurchasesEnabled()).isTrue();
        assertThat(service.getMonetizationSettings().isBoostPurchasesEnabled()).isTrue();
        assertThat(service.getModerationSettings().isUserBlockListEnabled()).isFalse();
        verify(publicCacheService).invalidate(PublicCacheService.CATALOG);
    }

    private static class InMemoryContentEntryGateway implements ContentEntryGateway {
        private final AtomicInteger ids = new AtomicInteger();
        private final Map<String, ContentEntry> entries = new LinkedHashMap<>();

        @Override
        public ContentEntry save(ContentEntry entry) {
            ContentEntry saved = entry;
            if (saved.getId() == null) {
                saved = saved.toBuilder().id("entry-" + ids.incrementAndGet()).build();
            }
            entries.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public List<ContentEntry> findAll() {
            return new ArrayList<>(entries.values());
        }

        @Override
        public List<ContentEntry> findByStatusAndLocale(ContentEntryStatus status, String locale) {
            return entries.values().stream()
                    .filter(entry -> entry.getStatus() == status)
                    .filter(entry -> locale.equals(entry.getLocale()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<ContentEntry> findByStatusAndLocaleAndKeyIn(ContentEntryStatus status, String locale, Collection<String> keys) {
            return findByStatusAndLocale(status, locale).stream()
                    .filter(entry -> keys.contains(entry.getKey()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public Optional<ContentEntry> findById(String id) {
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public Optional<ContentEntry> findByKeyAndLocale(String key, String locale) {
            return entries.values().stream()
                    .filter(entry -> key.equals(entry.getKey()))
                    .filter(entry -> locale.equals(entry.getLocale()))
                    .findFirst();
        }
    }

    private static class InMemoryContentRevisionGateway implements ContentRevisionGateway {
        private final List<ContentRevision> revisions = new ArrayList<>();

        @Override
        public ContentRevision save(ContentRevision revision) {
            revisions.add(revision);
            return revision;
        }

        @Override
        public List<ContentRevision> findByContentEntryId(String contentEntryId) {
            return revisions.stream()
                    .filter(revision -> contentEntryId.equals(revision.getContentEntryId()))
                    .collect(java.util.stream.Collectors.toList());
        }
    }
}

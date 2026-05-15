package com.euprocuro.api.application.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.CatalogCategoryCommand;
import com.euprocuro.api.application.command.CatalogProductCommand;
import com.euprocuro.api.application.command.ModerationSettingsCommand;
import com.euprocuro.api.application.command.MonetizationSettingsCommand;
import com.euprocuro.api.application.command.SaveOperationalCatalogCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.view.AdminOperationalCatalogView;
import com.euprocuro.api.application.view.CatalogCategoryView;
import com.euprocuro.api.application.view.ModerationSettingsView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.application.view.MonetizationSettingsView;
import com.euprocuro.api.domain.gateway.ContentEntryGateway;
import com.euprocuro.api.domain.gateway.ContentRevisionGateway;
import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentEntryType;
import com.euprocuro.api.domain.model.ContentRevision;
import com.euprocuro.api.domain.model.MonetizationProductType;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;
import lombok.Data;

@Service
@RequiredArgsConstructor
public class OperationalCatalogService {

    private static final String DEFAULT_LOCALE = "pt-BR";
    private static final String CATEGORY_KEY = "catalog.categories";
    private static final String PRODUCT_KEY = "catalog.monetization.products";
    private static final String MONETIZATION_SETTINGS_KEY = "catalog.monetization.settings";
    private static final String MODERATION_SETTINGS_KEY = "catalog.moderation.settings";
    private static final String CODE_PATTERN = "[A-Z0-9][A-Z0-9_-]{1,48}";

    private final AdminAccessService adminAccessService;
    private final ContentEntryGateway contentEntryGateway;
    private final ContentRevisionGateway contentRevisionGateway;
    private final ObjectMapper objectMapper;
    private final PublicCacheService publicCacheService;
    private final AtomicBoolean defaultsSeeded = new AtomicBoolean(false);

    @Value("${application.cache.public.catalog-ttl-seconds:300}")
    private long catalogCacheTtlSeconds = 300;

    public List<CatalogCategoryView> listActiveCategories() {
        return publicCacheService.getOrLoad(
                PublicCacheService.CATALOG,
                "active-categories",
                catalogCacheTtlSeconds,
                () -> listCategories().stream()
                        .filter(CatalogCategoryView::isActive)
                        .collect(Collectors.toList())
        );
    }

    public List<MonetizationProductView> listActiveProducts() {
        return publicCacheService.getOrLoad(
                PublicCacheService.CATALOG,
                "active-products",
                catalogCacheTtlSeconds,
                () -> filterProductsForSettings(listProducts(), getMonetizationSettings())
        );
    }

    public MonetizationSettingsView getMonetizationSettings() {
        ensureDefaultsSeeded();
        return loadEntry(MONETIZATION_SETTINGS_KEY)
                .map(ContentEntry::getPublishedValue)
                .filter(StringUtils::hasText)
                .map(this::readSettings)
                .orElseGet(this::defaultMonetizationSettings);
    }

    public ModerationSettingsView getModerationSettings() {
        ensureDefaultsSeeded();
        return loadEntry(MODERATION_SETTINGS_KEY)
                .map(ContentEntry::getPublishedValue)
                .filter(StringUtils::hasText)
                .map(this::readModerationSettings)
                .orElseGet(this::defaultModerationSettings);
    }

    public String requireActiveCategory(String code) {
        String normalizedCode = normalizeCode(code);
        boolean active = listActiveCategories().stream()
                .anyMatch(category -> category.getCode().equals(normalizedCode));
        if (!active) {
            throw new BusinessException("Categoria invalida ou inativa.");
        }
        return normalizedCode;
    }

    public AdminOperationalCatalogView getAdminCatalog(String currentUserId) {
        adminAccessService.requireAdmin(currentUserId);
        return buildAdminView();
    }

    public AdminOperationalCatalogView saveAdminCatalog(String currentUserId, SaveOperationalCatalogCommand command) {
        UserProfile admin = adminAccessService.requireAdmin(currentUserId);
        if (command == null) {
            throw new BusinessException("Informe o catalogo para salvar.");
        }

        List<CatalogCategoryView> categories = validateCategories(command.getCategories());
        List<MonetizationProductView> products = validateProducts(command.getProducts());
        MonetizationSettingsView monetizationSettings = validateMonetizationSettings(command.getMonetizationSettings());
        ModerationSettingsView moderationSettings = validateModerationSettings(command.getModerationSettings());

        saveCatalogEntry(CATEGORY_KEY, "Categorias de anuncios", categories, admin.getId());
        saveCatalogEntry(PRODUCT_KEY, "Produtos, planos e promocoes", products, admin.getId());
        saveCatalogEntry(MONETIZATION_SETTINGS_KEY, "Configuracao de monetizacao", monetizationSettings, admin.getId());
        saveCatalogEntry(MODERATION_SETTINGS_KEY, "Configuracao de moderacao", moderationSettings, admin.getId());
        publicCacheService.invalidate(PublicCacheService.CATALOG);
        return buildAdminView();
    }

    private AdminOperationalCatalogView buildAdminView() {
        ensureDefaultsSeeded();
        Instant updatedAt = Stream.of(
                        loadEntry(CATEGORY_KEY),
                        loadEntry(PRODUCT_KEY),
                        loadEntry(MONETIZATION_SETTINGS_KEY),
                        loadEntry(MODERATION_SETTINGS_KEY)
                )
                .flatMap(Optional::stream)
                .map(ContentEntry::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return AdminOperationalCatalogView.builder()
                .monetizationSettings(getMonetizationSettings())
                .moderationSettings(getModerationSettings())
                .categories(listCategories())
                .products(listProducts())
                .updatedAt(updatedAt)
                .build();
    }

    private List<CatalogCategoryView> listCategories() {
        ensureDefaultsSeeded();
        return loadPublished(CATEGORY_KEY, new TypeReference<List<CatalogCategoryRecord>>() {})
                .stream()
                .map(CatalogCategoryRecord::toView)
                .sorted(Comparator.comparing(category -> Optional.ofNullable(category.getSortOrder()).orElse(0)))
                .collect(Collectors.toList());
    }

    private List<MonetizationProductView> listProducts() {
        ensureDefaultsSeeded();
        return loadPublished(PRODUCT_KEY, new TypeReference<List<CatalogProductRecord>>() {})
                .stream()
                .map(CatalogProductRecord::toView)
                .sorted(Comparator.comparing(product -> Optional.ofNullable(product.getSortOrder()).orElse(0)))
                .collect(Collectors.toList());
    }

    private <T> List<T> loadPublished(String key, TypeReference<List<T>> typeReference) {
        return loadEntry(key)
                .map(ContentEntry::getPublishedValue)
                .filter(StringUtils::hasText)
                .map(value -> readList(value, typeReference))
                .orElseGet(List::of);
    }

    private <T> List<T> readList(String value, TypeReference<List<T>> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (IOException exception) {
            throw new IllegalStateException("Catalogo operacional invalido.", exception);
        }
    }

    private MonetizationSettingsView readSettings(String value) {
        try {
            return objectMapper.readValue(value, MonetizationSettingsRecord.class).toView();
        } catch (IOException exception) {
            throw new IllegalStateException("Configuracao de monetizacao invalida.", exception);
        }
    }

    private ModerationSettingsView readModerationSettings(String value) {
        try {
            return objectMapper.readValue(value, ModerationSettingsRecord.class).toView();
        } catch (IOException exception) {
            throw new IllegalStateException("Configuracao de moderacao invalida.", exception);
        }
    }

    private Optional<ContentEntry> loadEntry(String key) {
        ensureDefaultsSeeded();
        return contentEntryGateway.findByKeyAndLocale(key, DEFAULT_LOCALE);
    }

    private void ensureDefaultsSeeded() {
        if (!defaultsSeeded.compareAndSet(false, true)) {
            return;
        }
        seedCatalogEntry(CATEGORY_KEY, "Categorias de anuncios", defaultCategories());
        seedCatalogEntry(PRODUCT_KEY, "Produtos, planos e promocoes", defaultProducts());
        seedCatalogEntry(MONETIZATION_SETTINGS_KEY, "Configuracao de monetizacao", defaultMonetizationSettings());
        seedCatalogEntry(MODERATION_SETTINGS_KEY, "Configuracao de moderacao", defaultModerationSettings());
    }

    private void seedCatalogEntry(String key, String description, Object value) {
        if (contentEntryGateway.findByKeyAndLocale(key, DEFAULT_LOCALE).isPresent()) {
            return;
        }
        saveCatalogEntry(key, description, value, null);
    }

    private void saveCatalogEntry(String key, String description, Object value, String adminId) {
        String json = writeJson(value);
        Optional<ContentEntry> existing = contentEntryGateway.findByKeyAndLocale(key, DEFAULT_LOCALE);
        Instant now = Instant.now();
        ContentEntry saved = contentEntryGateway.save(ContentEntry.builder()
                .id(existing.map(ContentEntry::getId).orElse(null))
                .key(key)
                .type(ContentEntryType.CATALOG)
                .locale(DEFAULT_LOCALE)
                .status(ContentEntryStatus.PUBLISHED)
                .version(existing.map(ContentEntry::getVersion).orElse(0) + 1)
                .draftValue(json)
                .publishedValue(json)
                .description(description)
                .screen("catalog")
                .createdAt(existing.map(ContentEntry::getCreatedAt).orElse(now))
                .updatedAt(now)
                .publishedAt(now)
                .updatedBy(adminId)
                .publishedBy(adminId)
                .build());

        contentRevisionGateway.save(ContentRevision.builder()
                .contentEntryId(saved.getId())
                .key(saved.getKey())
                .locale(saved.getLocale())
                .version(saved.getVersion())
                .snapshotValue(saved.getPublishedValue())
                .publishedBy(adminId)
                .publishedAt(now)
                .build());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel serializar catalogo operacional.", exception);
        }
    }

    private List<CatalogCategoryView> validateCategories(List<CatalogCategoryCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessException("Cadastre pelo menos uma categoria.");
        }
        Set<String> codes = new LinkedHashSet<>();
        List<CatalogCategoryView> categories = new ArrayList<>();
        for (CatalogCategoryCommand command : commands) {
            String code = normalizeCode(command.getCode());
            if (!code.matches(CODE_PATTERN) || !codes.add(code)) {
                throw new BusinessException("Categoria duplicada ou com codigo invalido: " + code);
            }
            if (!StringUtils.hasText(command.getLabel())) {
                throw new BusinessException("Informe o nome da categoria " + code + ".");
            }
            categories.add(CatalogCategoryView.builder()
                    .code(code)
                    .label(command.getLabel().trim())
                    .active(command.isActive())
                    .sortOrder(Optional.ofNullable(command.getSortOrder()).orElse(categories.size() + 1))
                    .build());
        }
        if (categories.stream().noneMatch(CatalogCategoryView::isActive)) {
            throw new BusinessException("Mantenha pelo menos uma categoria ativa.");
        }
        return categories;
    }

    private List<MonetizationProductView> validateProducts(List<CatalogProductCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessException("Cadastre pelo menos um produto de monetizacao.");
        }
        Set<String> codes = new LinkedHashSet<>();
        List<MonetizationProductView> products = new ArrayList<>();
        for (CatalogProductCommand command : commands) {
            String code = normalizeCode(command.getCode());
            if (!code.matches(CODE_PATTERN) || !codes.add(code)) {
                throw new BusinessException("Produto duplicado ou com codigo invalido: " + code);
            }
            if (!StringUtils.hasText(command.getName()) || command.getType() == null) {
                throw new BusinessException("Informe nome e tipo do produto " + code + ".");
            }
            BigDecimal price = Optional.ofNullable(command.getPrice()).orElse(BigDecimal.ZERO);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Preco do produto nao pode ser negativo.");
            }
            BigDecimal originalPrice = command.isPromotional() ? command.getOriginalPrice() : null;
            if (originalPrice != null && originalPrice.compareTo(price) <= 0) {
                throw new BusinessException("Preco original da promocao deve ser maior que o preco atual.");
            }
            products.add(MonetizationProductView.builder()
                    .code(code)
                    .name(command.getName().trim())
                    .description(trimToNull(command.getDescription()))
                    .type(command.getType())
                    .price(price)
                    .originalPrice(originalPrice)
                    .promotional(command.isPromotional())
                    .promotionLabel(trimToNull(command.getPromotionLabel()))
                    .credits(command.getCredits())
                    .durationDays(command.getDurationDays())
                    .enabled(command.isEnabled())
                    .sortOrder(Optional.ofNullable(command.getSortOrder()).orElse(products.size() + 1))
                    .build());
        }
        return products;
    }

    private MonetizationSettingsView validateMonetizationSettings(MonetizationSettingsCommand command) {
        if (command == null) {
            return defaultMonetizationSettings();
        }
        return MonetizationSettingsView.builder()
                .creditPurchasesEnabled(command.isCreditPurchasesEnabled())
                .boostPurchasesEnabled(command.isBoostPurchasesEnabled())
                .build();
    }

    private ModerationSettingsView validateModerationSettings(ModerationSettingsCommand command) {
        if (command == null) {
            return defaultModerationSettings();
        }
        return ModerationSettingsView.builder()
                .userBlockListEnabled(command.isUserBlockListEnabled())
                .build();
    }

    private List<MonetizationProductView> filterProductsForSettings(
            List<MonetizationProductView> products,
            MonetizationSettingsView settings
    ) {
        boolean creditPurchasesEnabled = settings != null && settings.isCreditPurchasesEnabled();
        boolean boostPurchasesEnabled = settings != null && settings.isBoostPurchasesEnabled();
        return products.stream()
                .filter(MonetizationProductView::isEnabled)
                .filter(product -> {
                    if (product.getType() == MonetizationProductType.BOOST) {
                        return boostPurchasesEnabled;
                    }
                    if (product.getType() == MonetizationProductType.CREDIT_PACK
                            || product.getType() == MonetizationProductType.SUBSCRIPTION) {
                        return creditPurchasesEnabled;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private MonetizationSettingsView defaultMonetizationSettings() {
        return MonetizationSettingsView.builder()
                .creditPurchasesEnabled(false)
                .boostPurchasesEnabled(false)
                .build();
    }

    private ModerationSettingsView defaultModerationSettings() {
        return ModerationSettingsView.builder()
                .userBlockListEnabled(true)
                .build();
    }

    private List<CatalogCategoryView> defaultCategories() {
        return List.of(
                category("AUTOMOVEIS", "Automoveis", 10),
                category("IMOVEIS", "Imoveis", 20),
                category("SERVICOS", "Servicos", 30),
                category("ELETRONICOS", "Eletronicos", 40),
                category("INSTRUMENTOS", "Instrumentos", 50),
                category("OUTROS", "Outros", 60)
        );
    }

    private CatalogCategoryView category(String code, String label, int sortOrder) {
        return CatalogCategoryView.builder()
                .code(code)
                .label(label)
                .active(true)
                .sortOrder(sortOrder)
                .build();
    }

    private List<MonetizationProductView> defaultProducts() {
        return List.of(
                product("CREDITS_10", "10 propostas", "Pacote para vendedores enviarem propostas avulsas.",
                        MonetizationProductType.CREDIT_PACK, new BigDecimal("9.90"), 10, null, 10),
                product("CREDITS_30", "30 propostas", "Mais volume para vendedores frequentes.",
                        MonetizationProductType.CREDIT_PACK, new BigDecimal("24.90"), 30, null, 20),
                product("SELLER_PRO", "Plano vendedor Pro", "Propostas ilimitadas por 30 dias neste MVP.",
                        MonetizationProductType.SUBSCRIPTION, new BigDecimal("49.90"), null, 30, 30),
                product("BOOST_3_DAYS", "Boost 3 dias", "Impulsiona o interesse na busca e na home.",
                        MonetizationProductType.BOOST, new BigDecimal("9.90"), 3, 3, 40),
                product("BOOST_7_DAYS", "Boost 7 dias", "Mais tempo em destaque para receber propostas.",
                        MonetizationProductType.BOOST, new BigDecimal("19.90"), 6, 7, 50),
                product("BOOST_9_DAYS", "Boost 9 dias", "Destaque prolongado para procuras prioritarias.",
                        MonetizationProductType.BOOST, new BigDecimal("24.90"), 8, 9, 60)
        );
    }

    private MonetizationProductView product(
            String code,
            String name,
            String description,
            MonetizationProductType type,
            BigDecimal price,
            Integer credits,
            Integer durationDays,
            int sortOrder
    ) {
        return MonetizationProductView.builder()
                .code(code)
                .name(name)
                .description(description)
                .type(type)
                .price(price)
                .credits(credits)
                .durationDays(durationDays)
                .enabled(true)
                .sortOrder(sortOrder)
                .build();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Data
    private static class CatalogCategoryRecord {
        private String code;
        private String label;
        private boolean active;
        private Integer sortOrder;

        private CatalogCategoryView toView() {
            return CatalogCategoryView.builder()
                    .code(code)
                    .label(label)
                    .active(active)
                    .sortOrder(sortOrder)
                    .build();
        }
    }

    @Data
    private static class CatalogProductRecord {
        private String code;
        private String name;
        private String description;
        private MonetizationProductType type;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private boolean promotional;
        private String promotionLabel;
        private Integer credits;
        private Integer durationDays;
        private boolean enabled;
        private Integer sortOrder;

        private MonetizationProductView toView() {
            return MonetizationProductView.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .type(type)
                    .price(price)
                    .originalPrice(originalPrice)
                    .promotional(promotional)
                    .promotionLabel(promotionLabel)
                    .credits(credits)
                    .durationDays(durationDays)
                    .enabled(enabled)
                    .sortOrder(sortOrder)
                    .build();
        }
    }

    @Data
    private static class MonetizationSettingsRecord {
        private boolean creditPurchasesEnabled;
        private boolean boostPurchasesEnabled;

        private MonetizationSettingsView toView() {
            return MonetizationSettingsView.builder()
                    .creditPurchasesEnabled(creditPurchasesEnabled)
                    .boostPurchasesEnabled(boostPurchasesEnabled)
                    .build();
        }
    }

    @Data
    private static class ModerationSettingsRecord {
        private boolean userBlockListEnabled = true;

        private ModerationSettingsView toView() {
            return ModerationSettingsView.builder()
                    .userBlockListEnabled(userBlockListEnabled)
                    .build();
        }
    }
}

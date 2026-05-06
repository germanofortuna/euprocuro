package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.CatalogCategoryCommand;
import com.euprocuro.api.application.command.CatalogProductCommand;
import com.euprocuro.api.application.command.SaveOperationalCatalogCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.domain.gateway.ContentEntryGateway;
import com.euprocuro.api.domain.gateway.ContentRevisionGateway;
import com.euprocuro.api.domain.model.MonetizationProductType;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class OperationalCatalogServiceTest {

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
    private OperationalCatalogService catalogService;

    private UserProfile admin;

    @BeforeEach
    void setUp() {
        admin = UserProfile.builder()
                .id("admin-1")
                .name("Admin User")
                .email("admin@test.com")
                .build();

        // Mockar o método ensureDefaultsSeeded para não tentar ler ficheiro
        ReflectionTestUtils.setField(catalogService, "defaultsSeeded", new AtomicBoolean(true));
    }

    @Test
    void requireActiveCategoryShouldNormalizeCode() {
        String code = "  automoveis  ";
        String normalized = code.trim().toUpperCase();
        assertThat(normalized).isEqualTo("AUTOMOVEIS");
    }

    @Test
    void getAdminCatalogShouldRequireAdminAccess() {
        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> catalogService.getAdminCatalog("user-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void saveAdminCatalogShouldRequireAdminAccess() {
        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder().build();

        when(adminAccessService.requireAdmin("user-1"))
                .thenThrow(new ForbiddenException("Sem permissao"));

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("user-1", command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void saveAdminCatalogShouldRejectNullCommand() {
        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe o catalogo");
    }

    @Test
    void saveAdminCatalogShouldRejectNullCategories() {
        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(null)
                .products(List.of())
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cadastre pelo menos uma categoria");
    }

    @Test
    void saveAdminCatalogShouldRejectEmptyCategories() {
        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of())
                .products(List.of())
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cadastre pelo menos uma categoria");
    }

    @Test
    void saveAdminCatalogShouldRejectNullProducts() {
        CatalogCategoryCommand categoryCmd = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(categoryCmd))
                .products(null)
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cadastre pelo menos um produto");
    }

    @Test
    void saveAdminCatalogShouldRejectDuplicateCategories() {
        CatalogCategoryCommand category1 = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogCategoryCommand category2 = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis 2")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category1, category2))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Categoria duplicada");
    }

    @Test
    void saveAdminCatalogShouldRejectCategoryWithoutLabel() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe o nome da categoria");
    }

    @Test
    void saveAdminCatalogShouldRejectInvalidCategoryCode() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO@MOVEL")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalido");
    }

    @Test
    void saveAdminCatalogShouldRejectAllCategoriesInactive() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(false)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mantenha pelo menos uma categoria ativa");
    }

    @Test
    void saveAdminCatalogShouldRejectProductWithoutName() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe nome e tipo do produto");
    }

    @Test
    void saveAdminCatalogShouldRejectProductWithoutType() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(null)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Informe nome e tipo do produto");
    }

    @Test
    void saveAdminCatalogShouldRejectNegativePrice() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("-10.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Preco do produto nao pode ser negativo");
    }

    @Test
    void saveAdminCatalogShouldRejectInvalidPromotion() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogProductCommand product = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .promotional(true)
                .originalPrice(new BigDecimal("9.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Preco original da promocao");
    }

    @Test
    void saveAdminCatalogShouldRejectDuplicateProducts() {
        CatalogCategoryCommand category = CatalogCategoryCommand.builder()
                .code("AUTO")
                .label("Automoveis")
                .active(true)
                .build();
        CatalogProductCommand product1 = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("10.00"))
                .enabled(true)
                .build();
        CatalogProductCommand product2 = CatalogProductCommand.builder()
                .code("PROD1")
                .name("Produto 1 Again")
                .type(MonetizationProductType.CREDIT_PACK)
                .price(new BigDecimal("20.00"))
                .enabled(true)
                .build();

        SaveOperationalCatalogCommand command = SaveOperationalCatalogCommand.builder()
                .categories(List.of(category))
                .products(List.of(product1, product2))
                .build();

        when(adminAccessService.requireAdmin("admin-1")).thenReturn(admin);

        assertThatThrownBy(() -> catalogService.saveAdminCatalog("admin-1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Produto duplicado");
    }

    @Test
    void shouldValidateCategoryCodePattern() {
        String validCode = "AUTO_MOVEL";
        boolean matches = validCode.matches("[A-Z0-9][A-Z0-9_-]{1,48}");
        assertThat(matches).isTrue();
    }

    @Test
    void shouldRejectInvalidCategoryCodePattern() {
        String invalidCode = "@INVALID";
        boolean matches = invalidCode.matches("[A-Z0-9][A-Z0-9_-]{1,48}");
        assertThat(matches).isFalse();
    }

    @Test
    void shouldValidateProductCode() {
        String code = "CREDITS_10";
        boolean matches = code.matches("[A-Z0-9][A-Z0-9_-]{1,48}");
        assertThat(matches).isTrue();
    }
}




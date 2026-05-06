package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.model.MonetizationProductType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonetizationCatalogTest {

    @Mock
    private OperationalCatalogService operationalCatalogService;

    @Test
    void productsAndFindByCodeShouldDelegateToOperationalCatalog() {
        MonetizationCatalog catalog = new MonetizationCatalog(operationalCatalogService);
        MonetizationProductView product = MonetizationProductView.builder()
                .code("BOOST_3_DAYS")
                .name("Boost")
                .type(MonetizationProductType.BOOST)
                .price(new BigDecimal("9.90"))
                .enabled(true)
                .build();
        when(operationalCatalogService.listActiveProducts()).thenReturn(List.of(product));

        assertThat(catalog.products()).containsExactly(product);
        assertThat(catalog.findByCode("boost_3_days")).contains(product);
        assertThat(catalog.findByCode("missing")).isEmpty();
    }
}

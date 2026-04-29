package com.euprocuro.api.shared.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.model.MonetizationProductType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Component
@ConfigurationProperties(prefix = "application.monetization.catalog")
public class MonetizationCatalogProperties {

    private ProductProperties credits10 = new ProductProperties(
            "CREDITS_10",
            "10 propostas",
            "Pacote para vendedores enviarem propostas avulsas.",
            MonetizationProductType.CREDIT_PACK,
            new BigDecimal("9.90"),
            10,
            null,
            true
    );
    private ProductProperties credits30 = new ProductProperties(
            "CREDITS_30",
            "30 propostas",
            "Mais volume para vendedores frequentes.",
            MonetizationProductType.CREDIT_PACK,
            new BigDecimal("24.90"),
            30,
            null,
            true
    );
    private ProductProperties sellerPro = new ProductProperties(
            "SELLER_PRO",
            "Plano vendedor Pro",
            "Propostas ilimitadas por 30 dias neste MVP.",
            MonetizationProductType.SUBSCRIPTION,
            new BigDecimal("49.90"),
            null,
            30,
            true
    );
    private ProductProperties boost3Days = new ProductProperties(
            "BOOST_3_DAYS",
            "Boost 3 dias",
            "Impulsiona o interesse na busca e na home.",
            MonetizationProductType.BOOST,
            new BigDecimal("9.90"),
            null,
            3,
            true
    );
    private ProductProperties boost7Days = new ProductProperties(
            "BOOST_7_DAYS",
            "Boost 7 dias",
            "Mais tempo em destaque para receber propostas.",
            MonetizationProductType.BOOST,
            new BigDecimal("19.90"),
            null,
            7,
            true
    );

    public List<MonetizationProductView> toProducts() {
        return List.of(credits10, credits30, sellerPro, boost3Days, boost7Days)
                .stream()
                .filter(Objects::nonNull)
                .filter(ProductProperties::isEnabled)
                .map(ProductProperties::toView)
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductProperties {
        private String code;
        private String name;
        private String description;
        private MonetizationProductType type;
        private BigDecimal price;
        private Integer credits;
        private Integer durationDays;
        private boolean enabled = true;

        private MonetizationProductView toView() {
            return MonetizationProductView.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .type(type)
                    .price(price)
                    .credits(credits)
                    .durationDays(durationDays)
                    .build();
        }
    }
}

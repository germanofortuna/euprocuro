package com.euprocuro.api.entrypoints.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.euprocuro.api.domain.model.InterestModeration;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;

class RestMapperTest {

    @Test
    void toPublicInterestResponseShouldHideSensitiveFields() throws JsonProcessingException {
        InterestPost interest = InterestPost.builder()
                .id("interest-1")
                .ownerId("buyer-1")
                .ownerName("Ana Buyer")
                .title("Procuro violao")
                .referenceImageUrl("data:image/png;base64,abc")
                .location(LocationInfo.builder()
                        .postalCode("13010-111")
                        .city("Campinas")
                        .state("SP")
                        .neighborhood("Centro")
                        .country("Brasil")
                        .build())
                .allowsWhatsappContact(true)
                .whatsappContact("11999999999")
                .status(InterestStatus.APPROVED)
                .moderation(InterestModeration.builder()
                        .riskLevel(ModerationRiskLevel.LOW)
                        .provider("openai")
                        .categories(Map.of("violence", false))
                        .scores(Map.of("violence", 0.01))
                        .checkedAt(Instant.now())
                        .build())
                .build();

        InterestResponse response = RestMapper.toPublicInterestResponse(interest);

        assertThat(response.getOwnerId()).isNull();
        assertThat(response.getOwnerName()).isNull();
        assertThat(response.getReferenceImageUrl()).isEqualTo("data:image/png;base64,abc");
        assertThat(response.getWhatsappContact()).isNull();
        assertThat(response.isAllowsWhatsappContact()).isFalse();
        assertThat(response.getModeration()).isNull();
        assertThat(response.getStatus()).isEqualTo(InterestStatus.OPEN);
        assertThat(response.getLocation().getPostalCode()).isNull();
        assertThat(response.getLocation().getNeighborhood()).isNull();
        assertThat(response.getLocation().getCity()).isEqualTo("Campinas");
        assertThat(response.getLocation().getState()).isEqualTo("SP");

        String json = new ObjectMapper().writeValueAsString(response);
        assertThat(json)
                .doesNotContain("ownerId")
                .doesNotContain("ownerName")
                .doesNotContain("whatsappContact")
                .doesNotContain("moderation")
                .doesNotContain("postalCode")
                .doesNotContain("neighborhood");
    }

    @Test
    void toPublicInterestResponseShouldHideUnsafeReferenceImages() {
        InterestPost svgDataImage = InterestPost.builder()
                .id("interest-2")
                .title("Procuro imagem")
                .referenceImageUrl("data:image/svg+xml;base64,abc")
                .status(InterestStatus.APPROVED)
                .build();
        InterestPost javascriptImage = InterestPost.builder()
                .id("interest-3")
                .title("Procuro imagem")
                .referenceImageUrl("javascript:alert(1)")
                .status(InterestStatus.APPROVED)
                .build();

        assertThat(RestMapper.toPublicInterestResponse(svgDataImage).getReferenceImageUrl()).isNull();
        assertThat(RestMapper.toPublicInterestResponse(javascriptImage).getReferenceImageUrl()).isNull();
    }
}

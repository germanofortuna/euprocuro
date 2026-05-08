package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.UserProfile;

class InterestDeliveryRankingServiceTest {

    private final InterestDeliveryRankingService service = new InterestDeliveryRankingService();

    @Test
    void rankShouldPrioritizeSellerItemTextAndLocationMatchesOverOnlyRecentPosts() {
        UserProfile user = UserProfile.builder()
                .city("Erechim")
                .state("RS")
                .country("Brasil")
                .build();
        SellerItem item = SellerItem.builder()
                .title("Perfume Davidoff Cool Water")
                .description("Perfume masculino")
                .category("OUTROS")
                .tags(List.of("perfume", "davidoff"))
                .location(LocationInfo.builder().city("Erechim").state("RS").country("Brasil").build())
                .active(true)
                .build();
        InterestPost recentGeneric = interest("generic", "Procuro cadeira", "Quero uma cadeira", "MOVEIS",
                "Porto Alegre", "RS", List.of("cadeira"), Instant.now());
        InterestPost olderMatch = interest("match", "Procuro Davidoff Cool Water", "Pode ser usado",
                "OUTROS", "Erechim", "RS", List.of("perfume"), Instant.now().minus(20, ChronoUnit.DAYS));

        List<InterestPost> ranked = service.rank(List.of(recentGeneric, olderMatch), Optional.of(user), List.of(item));

        assertThat(ranked).extracting(InterestPost::getId).containsExactly("match", "generic");
    }

    @Test
    void rankShouldStillPrioritizeBoostWhenThereIsNoPersonalContext() {
        InterestPost boosted = interest("boosted", "Procuro notebook", "Notebook usado", "ELETRONICOS",
                "Canoas", "RS", List.of("notebook"), Instant.now().minus(10, ChronoUnit.DAYS));
        boosted.setBoostedUntil(Instant.now().plus(1, ChronoUnit.DAYS));
        InterestPost recent = interest("recent", "Procuro mesa", "Mesa pequena", "MOVEIS",
                "Canoas", "RS", List.of("mesa"), Instant.now());

        List<InterestPost> ranked = service.rank(List.of(recent, boosted), Optional.empty(), List.of());

        assertThat(ranked).extracting(InterestPost::getId).containsExactly("boosted", "recent");
    }

    private InterestPost interest(String id, String title, String description, String category, String city,
                                  String state, List<String> tags, Instant createdAt) {
        return InterestPost.builder()
                .id(id)
                .title(title)
                .description(description)
                .category(category)
                .location(LocationInfo.builder().city(city).state(state).country("Brasil").build())
                .tags(tags)
                .createdAt(createdAt)
                .build();
    }
}

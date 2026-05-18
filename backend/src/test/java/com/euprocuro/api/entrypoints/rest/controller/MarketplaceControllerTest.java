package com.euprocuro.api.entrypoints.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.service.OperationalCatalogService;
import com.euprocuro.api.application.usecase.DashboardUseCase;
import com.euprocuro.api.application.usecase.MarketplaceUseCase;
import com.euprocuro.api.application.usecase.ModerationUseCase;
import com.euprocuro.api.domain.model.InterestModeration;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class MarketplaceControllerTest {

    @Mock
    private MarketplaceUseCase marketplaceUseCase;
    @Mock
    private DashboardUseCase dashboardUseCase;
    @Mock
    private ModerationUseCase moderationUseCase;
    @Mock
    private OperationalCatalogService operationalCatalogService;

    @InjectMocks
    private MarketplaceController controller;

    @Test
    void listInterestsShouldForcePublicVisibilityAndSanitizeResponse() {
        InterestPost interest = publicInterest("interest-1", "buyer-1");
        when(marketplaceUseCase.listInterests(any(InterestSearchFilter.class), eq(0), eq(10)))
                .thenReturn(List.of(interest));

        List<InterestResponse> response = controller.listInterests(
                new MockHttpServletRequest(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                0,
                10
        );

        ArgumentCaptor<InterestSearchFilter> filterCaptor = ArgumentCaptor.forClass(InterestSearchFilter.class);
        org.mockito.Mockito.verify(marketplaceUseCase).listInterests(filterCaptor.capture(), eq(0), eq(10));

        assertThat(filterCaptor.getValue().isOpenOnly()).isTrue();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getStatus()).isEqualTo(InterestStatus.OPEN);
        assertThat(response.get(0).getModeration()).isNull();
        assertThat(response.get(0).getOwnerId()).isNull();
        assertThat(response.get(0).getLocation().getPostalCode()).isNull();
        assertThat(response.get(0).getLocation().getNeighborhood()).isNull();
    }

    @Test
    void listInterestsShouldExcludeCurrentUsersOwnInterests() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, "buyer-1");
        when(marketplaceUseCase.listInterests(any(InterestSearchFilter.class), eq(0), eq(10)))
                .thenReturn(List.of(
                        publicInterest("own-interest", "buyer-1"),
                        publicInterest("other-interest", "buyer-2")
                ));

        List<InterestResponse> response = controller.listInterests(
                request,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                0,
                10
        );

        ArgumentCaptor<InterestSearchFilter> filterCaptor = ArgumentCaptor.forClass(InterestSearchFilter.class);
        org.mockito.Mockito.verify(marketplaceUseCase).listInterests(filterCaptor.capture(), eq(0), eq(10));

        assertThat(filterCaptor.getValue().getCurrentUserId()).isNull();
        assertThat(response).extracting(InterestResponse::getId).containsExactly("other-interest");
    }

    private InterestPost publicInterest(String id, String ownerId) {
        return InterestPost.builder()
                .id(id)
                .ownerId(ownerId)
                .ownerName("Buyer")
                .title("Procuro violao")
                .status(InterestStatus.APPROVED)
                .location(LocationInfo.builder()
                        .postalCode("13010-111")
                        .city("Campinas")
                        .state("SP")
                        .neighborhood("Centro")
                        .country("Brasil")
                        .build())
                .moderation(InterestModeration.builder()
                        .riskLevel(ModerationRiskLevel.LOW)
                        .provider("openai")
                        .categories(Map.of("violence", false))
                        .scores(Map.of("violence", 0.01))
                        .build())
                .build();
    }
}

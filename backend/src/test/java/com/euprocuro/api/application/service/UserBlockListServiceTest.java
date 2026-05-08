package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.view.ModerationSettingsView;
import com.euprocuro.api.domain.gateway.UserBlockListGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.UserBlockListEntry;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class UserBlockListServiceTest {

    @Mock
    private UserBlockListGateway userBlockListGateway;
    @Mock
    private OperationalCatalogService operationalCatalogService;

    @InjectMocks
    private UserBlockListService userBlockListService;

    @Test
    void blockShouldNormalizeDocumentAndCreateActiveEntry() {
        when(operationalCatalogService.getModerationSettings()).thenReturn(enabledSettings());
        when(userBlockListGateway.findByDocumentHash(anyString())).thenReturn(Optional.empty());
        when(userBlockListGateway.save(any(UserBlockListEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<UserBlockListEntry> result = userBlockListService.block(user(), interest(), "OPENAI", "Rejeitado");

        assertThat(result).isPresent();
        ArgumentCaptor<UserBlockListEntry> captor = ArgumentCaptor.forClass(UserBlockListEntry.class);
        verify(userBlockListGateway).save(captor.capture());
        UserBlockListEntry saved = captor.getValue();
        assertThat(saved.getDocumentHash()).isNotBlank();
        assertThat(saved.getDocumentHash()).isNotEqualTo("12345678901");
        assertThat(saved.getDocumentLast4()).isEqualTo("8901");
        assertThat(saved.getDocumentType()).isEqualTo("CPF");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getOccurrenceCount()).isEqualTo(1);
        assertThat(saved.getSourceInterestId()).isEqualTo("interest-1");
    }

    @Test
    void blockShouldReuseExistingEntryAndIncrementOccurrenceCount() {
        Instant firstBlockedAt = Instant.parse("2026-05-01T10:00:00Z");
        UserBlockListEntry existing = UserBlockListEntry.builder()
                .id("block-1")
                .documentHash("existing-hash")
                .documentLast4("8901")
                .occurrenceCount(2)
                .firstBlockedAt(firstBlockedAt)
                .createdAt(firstBlockedAt)
                .active(false)
                .build();
        when(operationalCatalogService.getModerationSettings()).thenReturn(enabledSettings());
        when(userBlockListGateway.findByDocumentHash(anyString())).thenReturn(Optional.of(existing));
        when(userBlockListGateway.save(any(UserBlockListEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<UserBlockListEntry> result = userBlockListService.block(user(), interest(), "LOCAL_RULE", "Termo bloqueado");

        assertThat(result).isPresent();
        ArgumentCaptor<UserBlockListEntry> captor = ArgumentCaptor.forClass(UserBlockListEntry.class);
        verify(userBlockListGateway).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("block-1");
        assertThat(captor.getValue().getOccurrenceCount()).isEqualTo(3);
        assertThat(captor.getValue().getFirstBlockedAt()).isEqualTo(firstBlockedAt);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void findActiveBlockShouldReturnEmptyWhenFeatureIsDisabled() {
        when(operationalCatalogService.getModerationSettings()).thenReturn(ModerationSettingsView.builder()
                .userBlockListEnabled(false)
                .build());

        Optional<UserBlockListEntry> result = userBlockListService.findActiveBlock(user());

        assertThat(result).isEmpty();
        verify(userBlockListGateway, never()).findByDocumentHashAndActiveTrue(any());
    }

    @Test
    void blockShouldReturnEmptyWhenUserHasNoDocument() {
        when(operationalCatalogService.getModerationSettings()).thenReturn(enabledSettings());

        Optional<UserBlockListEntry> result = userBlockListService.block(user().toBuilder()
                .documentNumber(null)
                .build(), interest(), "OPENAI", "Rejeitado");

        assertThat(result).isEmpty();
        verify(userBlockListGateway, never()).save(any());
    }

    private ModerationSettingsView enabledSettings() {
        return ModerationSettingsView.builder()
                .userBlockListEnabled(true)
                .build();
    }

    private UserProfile user() {
        return UserProfile.builder()
                .id("buyer-1")
                .name("Ana")
                .email("ana@teste.com")
                .documentNumber("123.456.789-01")
                .documentType("CPF")
                .build();
    }

    private InterestPost interest() {
        return InterestPost.builder()
                .id("interest-1")
                .ownerId("buyer-1")
                .title("Procuro item")
                .build();
    }
}

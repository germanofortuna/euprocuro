package com.euprocuro.api.application.usecase;

import java.util.List;

import com.euprocuro.api.application.command.SaveContentEntryCommand;
import com.euprocuro.api.application.view.AdminContentCatalogView;
import com.euprocuro.api.application.view.ContentEntryView;
import com.euprocuro.api.application.view.ContentRevisionView;

public interface AdminContentUseCase {
    AdminContentCatalogView getContentEntries(String currentUserId);

    ContentEntryView saveDraft(String currentUserId, String entryId, SaveContentEntryCommand command);

    ContentEntryView publish(String currentUserId, String entryId);

    ContentEntryView archive(String currentUserId, String entryId);

    ContentEntryView applyDefaultDraft(String currentUserId, String entryId);

    ContentEntryView dismissDefaultUpdate(String currentUserId, String entryId);

    List<ContentRevisionView> getRevisions(String currentUserId, String entryId);
}

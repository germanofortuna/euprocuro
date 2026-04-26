package com.euprocuro.api.application.usecase;

import java.util.List;

import com.euprocuro.api.application.command.CreateSellerItemCommand;
import com.euprocuro.api.application.command.ShareSellerItemCommand;
import com.euprocuro.api.application.command.UpdateSellerItemCommand;
import com.euprocuro.api.application.view.SellerItemMatchesView;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.SellerItem;

public interface SellerItemUseCase {
    List<SellerItemMatchesView> listItemsWithMatches(String currentUserId);

    SellerItem createItem(String currentUserId, CreateSellerItemCommand command);

    SellerItem updateItem(String currentUserId, String itemId, UpdateSellerItemCommand command);

    SellerItem deactivateItem(String currentUserId, String itemId);

    Offer shareItemAsOffer(String currentUserId, String itemId, String interestId, ShareSellerItemCommand command);
}

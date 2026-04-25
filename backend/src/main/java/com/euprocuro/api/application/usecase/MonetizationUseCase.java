package com.euprocuro.api.application.usecase;

import java.util.List;

import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.model.InterestPost;

public interface MonetizationUseCase {
    List<MonetizationProductView> listProducts();

    MonetizationAccountView getAccount(String userId);

    CheckoutView purchase(String userId, PurchaseProductCommand command);

    InterestPost boostInterest(String userId, String interestId, BoostInterestCommand command);
}

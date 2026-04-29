package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.domain.model.UserProfile;

public interface PaymentCheckoutGateway {
    CheckoutView createCheckout(UserProfile user, MonetizationProductView product, PaymentOrder paymentOrder);
}

package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.SellerItem;

public interface SellerItemGateway {
    SellerItem save(SellerItem item);

    List<SellerItem> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<SellerItem> findById(String id);
}

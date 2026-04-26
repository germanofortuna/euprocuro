package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.infrastructure.persistence.mapper.SellerItemPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataSellerItemRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SellerItemGatewayAdapter implements SellerItemGateway {

    private final SpringDataSellerItemRepository repository;

    @Override
    public SellerItem save(SellerItem item) {
        return SellerItemPersistenceMapper.toDomain(repository.save(SellerItemPersistenceMapper.toDocument(item)));
    }

    @Override
    public List<SellerItem> findByOwnerIdOrderByCreatedAtDesc(String ownerId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(SellerItemPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SellerItem> findById(String id) {
        return repository.findById(id).map(SellerItemPersistenceMapper::toDomain);
    }
}

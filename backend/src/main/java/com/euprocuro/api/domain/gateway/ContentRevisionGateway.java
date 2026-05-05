package com.euprocuro.api.domain.gateway;

import java.util.List;

import com.euprocuro.api.domain.model.ContentRevision;

public interface ContentRevisionGateway {
    ContentRevision save(ContentRevision revision);

    List<ContentRevision> findByContentEntryId(String contentEntryId);
}

package com.undercontroll.domain.gateway;

import java.time.Instant;
import java.util.Optional;

public interface ComponentAliasGateway {

    Optional<Integer> find(String supplierKey, String productKey);

    void upsert(String supplierKey, String productKey, Integer componentId, Instant now);
}

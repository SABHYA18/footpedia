package com.footpedia.repository;

import com.footpedia.domain.RawPayload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RawPayloadRepository extends JpaRepository<RawPayload, Long> {
    Optional<RawPayload> findByProviderAndIdempotencyKey(String provider, String idempotencyKey);
}

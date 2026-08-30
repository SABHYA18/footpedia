package com.footpedia.repository;

import com.footpedia.domain.ExternalRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalRefRepository extends JpaRepository<ExternalRef, Long> {
    Optional<ExternalRef> findByEntityTypeAndProviderAndExternalId(
            String entityType, String provider, String externalId);
}

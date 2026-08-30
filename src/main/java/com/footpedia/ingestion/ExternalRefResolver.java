package com.footpedia.ingestion;

import com.footpedia.domain.ExternalRef;
import com.footpedia.repository.ExternalRefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Translates between a provider's external ID and the internal surrogate ID
 * for the same real-world entity. Providers use different ID spaces, so
 * every cross-provider join goes through this table instead of a shared key.
 */
@Component
@RequiredArgsConstructor
public class ExternalRefResolver {

    private final ExternalRefRepository repository;

    public Optional<Long> resolve(String entityType, String provider, String externalId) {
        return repository.findByEntityTypeAndProviderAndExternalId(entityType, provider, externalId)
                .map(ExternalRef::getInternalId);
    }

    public void link(String entityType, String provider, String externalId, Long internalId) {
        ExternalRef ref = repository.findByEntityTypeAndProviderAndExternalId(entityType, provider, externalId)
                .orElseGet(ExternalRef::new);
        ref.setEntityType(entityType);
        ref.setProvider(provider);
        ref.setExternalId(externalId);
        ref.setInternalId(internalId);
        repository.save(ref);
    }
}

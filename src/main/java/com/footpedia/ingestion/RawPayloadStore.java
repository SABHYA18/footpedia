package com.footpedia.ingestion;

import com.footpedia.domain.RawPayload;
import com.footpedia.repository.RawPayloadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Persists every provider response before it is parsed, so the database can
 * be re-derived at zero API cost if parsing logic turns out wrong.
 *
 * <p>Fetches of the same endpoint within the same minute collapse onto one
 * row (idempotency key includes a minute bucket) so an overlapping scheduled
 * run can't duplicate storage; each new polling minute still gets its own
 * append-only row.
 *
 * <p>Always runs in its own transaction ({@code REQUIRES_NEW}): a fetch is
 * usually called from inside a larger ingestion transaction, and if that
 * transaction later rolls back (e.g. the provider returned an error, or
 * parsing failed), the raw response must still survive — that's the entire
 * point of storing it before parsing.
 */
@Component
@RequiredArgsConstructor
public class RawPayloadStore {

    private final RawPayloadRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RawPayload store(String provider, String endpoint, String requestParams, int httpStatus, String payload) {
        String idempotencyKey = buildIdempotencyKey(endpoint, requestParams);
        return repository.findByProviderAndIdempotencyKey(provider, idempotencyKey)
                .orElseGet(() -> {
                    RawPayload raw = new RawPayload();
                    raw.setProvider(provider);
                    raw.setEndpoint(endpoint);
                    raw.setRequestParams(requestParams);
                    raw.setIdempotencyKey(idempotencyKey);
                    raw.setHttpStatus(httpStatus);
                    raw.setPayload(payload);
                    raw.setFetchedAt(Instant.now());
                    return repository.save(raw);
                });
    }

    private String buildIdempotencyKey(String endpoint, String requestParams) {
        String minuteBucket = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        return "%s|%s|%s".formatted(endpoint, requestParams == null ? "" : requestParams, minuteBucket);
    }
}

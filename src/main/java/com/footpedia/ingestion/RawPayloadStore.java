package com.footpedia.ingestion;

import com.footpedia.domain.RawPayload;
import com.footpedia.repository.RawPayloadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
 *
 * <p>The lookup-then-insert has a real race window: two schedulers can
 * legitimately hit the same endpoint in the same minute (e.g. a
 * just-finished fixture that both {@code EspnMatchDetailScheduler}'s 3-hour
 * lookback and {@code EspnHistoricalBackfillScheduler}'s unbounded one pick
 * up at once), both pass the "not found" check before either commits, and
 * the second insert then violates the idempotency unique constraint. No
 * caller uses this method's result (every call site is fire-and-forget), so
 * losing that race just means skipping a redundant insert of a row that
 * already exists in essence -- not an error worth surfacing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RawPayloadStore {

    private final RawPayloadRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void store(String provider, String endpoint, String requestParams, int httpStatus, String payload) {
        String idempotencyKey = buildIdempotencyKey(endpoint, requestParams);
        if (repository.findByProviderAndIdempotencyKey(provider, idempotencyKey).isPresent()) {
            return;
        }

        RawPayload raw = new RawPayload();
        raw.setProvider(provider);
        raw.setEndpoint(endpoint);
        raw.setRequestParams(requestParams);
        raw.setIdempotencyKey(idempotencyKey);
        raw.setHttpStatus(httpStatus);
        raw.setPayload(payload);
        raw.setFetchedAt(Instant.now());
        try {
            repository.save(raw);
        } catch (DataIntegrityViolationException e) {
            log.debug("Lost the race to store {} {} -- another caller already stored this minute's fetch", provider, endpoint);
        }
    }

    private String buildIdempotencyKey(String endpoint, String requestParams) {
        String minuteBucket = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        return "%s|%s|%s".formatted(endpoint, requestParams == null ? "" : requestParams, minuteBucket);
    }
}

package com.footpedia.ingestion;

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
 * append-only row. The insert is a native {@code ON CONFLICT DO NOTHING}
 * upsert rather than a JPA check-then-insert -- two schedulers can
 * legitimately hit the same endpoint in the same minute (e.g. a
 * just-finished fixture both {@code EspnMatchDetailScheduler}'s 3-hour
 * lookback and {@code EspnHistoricalBackfillScheduler}'s unbounded one pick
 * up at once), and a plain "does it exist, if not insert" check-then-act
 * can't survive that concurrently. See {@link com.footpedia.repository.RawPayloadRepository#upsert}
 * for why catching the resulting constraint violation in Java doesn't work
 * around it either.
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
    public void store(String provider, String endpoint, String requestParams, int httpStatus, String payload) {
        String idempotencyKey = buildIdempotencyKey(endpoint, requestParams);
        repository.upsert(provider, endpoint, requestParams, idempotencyKey, httpStatus, payload, Instant.now());
    }

    private String buildIdempotencyKey(String endpoint, String requestParams) {
        String minuteBucket = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        return "%s|%s|%s".formatted(endpoint, requestParams == null ? "" : requestParams, minuteBucket);
    }
}

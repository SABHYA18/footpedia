package com.footpedia.repository;

import com.footpedia.domain.RawPayload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RawPayloadRepository extends JpaRepository<RawPayload, Long> {
    Optional<RawPayload> findByProviderAndIdempotencyKey(String provider, String idempotencyKey);

    /**
     * Atomic upsert: lets Postgres itself resolve the "does this idempotency
     * key already exist" race via {@code ON CONFLICT}, rather than a JPA
     * check-then-insert that can violate the unique constraint under
     * concurrent callers. Catching that violation in Java doesn't actually
     * work around it either -- Hibernate marks the enclosing transaction
     * rollback-only the moment the constraint violation happens, regardless
     * of whether the Java exception gets caught, so the caller's transaction
     * still fails at commit with {@code UnexpectedRollbackException}
     * (confirmed live: this recurred even after adding a try/catch). A
     * native conflict-free upsert never raises that exception in the first
     * place.
     */
    @Modifying
    @Query(
            value = """
            insert into raw_payload (provider, endpoint, request_params, idempotency_key, http_status, payload, fetched_at)
            values (:provider, :endpoint, :requestParams, :idempotencyKey, :httpStatus, :payload, :fetchedAt)
            on conflict (provider, idempotency_key) do nothing
            """,
            nativeQuery = true)
    void upsert(
            @Param("provider") String provider,
            @Param("endpoint") String endpoint,
            @Param("requestParams") String requestParams,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("httpStatus") int httpStatus,
            @Param("payload") String payload,
            @Param("fetchedAt") Instant fetchedAt);
}

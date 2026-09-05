package com.footpedia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only landing zone. Every provider response is stored here, raw,
 * before it is parsed — lets the whole database be re-derived at zero API
 * cost when parsing logic turns out wrong.
 */
@Entity
@Table(name = "raw_payload")
@Getter
@Setter
@NoArgsConstructor
public class RawPayload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "request_params", length = 512)
    private String requestParams;

    /** Same logical fetch must not be stored twice. */
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "http_status")
    private Integer httpStatus;

    // Plain text, not a json column: a provider error response (an HTML block
    // page, a truncated body) isn't valid JSON, and this table's whole job is
    // to survive exactly that kind of response for replay -- a JSON-typed
    // column would reject the insert and lose it. (Found via ESPN's Akamai
    // block page failing to insert here.)
    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}

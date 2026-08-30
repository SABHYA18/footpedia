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

/**
 * Maps a provider's external ID to the internal surrogate ID for the same
 * real-world entity. football-data.org and API-Football use different ID
 * spaces; translation happens only at the edges via this table.
 */
@Entity
@Table(name = "external_ref")
@Getter
@Setter
@NoArgsConstructor
public class ExternalRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "internal_id", nullable = false)
    private Long internalId;

    @Column(length = 16)
    private String confidence = "EXACT";
}

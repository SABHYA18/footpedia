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
 * One timeline entry (goal, card, substitution, ...) for a fixture. For a
 * substitution, {@code playerId} is the player coming on and
 * {@code relatedPlayerId} is the player going off.
 */
@Entity
@Table(name = "fixture_event")
@Getter
@Setter
@NoArgsConstructor
public class FixtureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixture_id", nullable = false)
    private Long fixtureId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "related_player_id")
    private Long relatedPlayerId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    private Integer minute;

    @Column(length = 256)
    private String detail;
}

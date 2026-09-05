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

@Entity
@Table(name = "fixture_lineup")
@Getter
@Setter
@NoArgsConstructor
public class FixtureLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixture_id", nullable = false)
    private Long fixtureId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(length = 16)
    private String position;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    /** ESPN's 1-11 role slot for a starter (stable convention, e.g. 1=GK, 9=striker); null for substitutes. */
    @Column(name = "formation_place")
    private Integer formationPlace;

    @Column(nullable = false)
    private boolean starter;

    @Column(name = "subbed_in", nullable = false)
    private boolean subbedIn;

    @Column(name = "subbed_out", nullable = false)
    private boolean subbedOut;
}

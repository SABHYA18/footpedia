package com.footpedia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "fixture")
@Getter
@Setter
@NoArgsConstructor
public class Fixture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Column(nullable = false, length = 16)
    private String season;

    private Integer matchday;

    @Column(name = "utc_kickoff", nullable = false)
    private Instant utcKickoff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FixtureStatus status;

    @Column(name = "home_team_id", nullable = false)
    private Long homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private Long awayTeamId;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_formation", length = 16)
    private String homeFormation;

    @Column(name = "away_formation", length = 16)
    private String awayFormation;

    /** Set once match detail (lineups/events/team stats) has been fetched for this match. */
    @Column(name = "detail_fetched_at")
    private Instant detailFetchedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

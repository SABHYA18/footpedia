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
 * One (fixture, team, stat type) row -- e.g. ("Total Shots", "15"). Deliberately
 * EAV rather than named columns: API-Football's stat list isn't fixed across
 * matches/competitions, and this shape stores whatever it returns unchanged.
 */
@Entity
@Table(name = "fixture_team_stat")
@Getter
@Setter
@NoArgsConstructor
public class FixtureTeamStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixture_id", nullable = false)
    private Long fixtureId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "stat_type", nullable = false, length = 64)
    private String statType;

    @Column(name = "stat_value", length = 32)
    private String statValue;
}

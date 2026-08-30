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

import java.math.BigDecimal;

@Entity
@Table(name = "fixture_player_stat")
@Getter
@Setter
@NoArgsConstructor
public class FixturePlayerStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixture_id", nullable = false)
    private Long fixtureId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(length = 8)
    private String position;

    private Integer minutes;

    private BigDecimal rating;

    private Integer goals;

    private Integer assists;

    @Column(name = "shots_total")
    private Integer shotsTotal;

    @Column(name = "shots_on")
    private Integer shotsOn;

    @Column(name = "passes_total")
    private Integer passesTotal;

    @Column(name = "passes_accuracy")
    private Integer passesAccuracy;

    private Integer tackles;

    @Column(name = "duels_total")
    private Integer duelsTotal;

    @Column(name = "duels_won")
    private Integer duelsWon;

    @Column(name = "yellow_cards")
    private Integer yellowCards;

    @Column(name = "red_cards")
    private Integer redCards;
}

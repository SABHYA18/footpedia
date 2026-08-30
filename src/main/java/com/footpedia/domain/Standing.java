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

@Entity
@Table(name = "standing")
@Getter
@Setter
@NoArgsConstructor
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Column(nullable = false, length = 16)
    private String season;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    private Integer position;
    private Integer played;
    private Integer won;
    private Integer draw;
    private Integer lost;
    private Integer points;

    @Column(name = "goals_for")
    private Integer goalsFor;

    @Column(name = "goals_against")
    private Integer goalsAgainst;

    @Column(length = 16)
    private String form;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

package com.footpedia.web.dto;

import com.footpedia.domain.Fixture;

import java.time.Instant;

public record FixtureResponse(
        Long id,
        Instant utcKickoff,
        String status,
        Integer matchday,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        Integer homeScore,
        Integer awayScore) {

    public static FixtureResponse from(Fixture f, TeamResponse homeTeam, TeamResponse awayTeam) {
        return new FixtureResponse(
                f.getId(),
                f.getUtcKickoff(),
                f.getStatus().name(),
                f.getMatchday(),
                homeTeam,
                awayTeam,
                f.getHomeScore(),
                f.getAwayScore());
    }
}

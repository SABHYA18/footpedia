package com.footpedia.web.dto;

import com.footpedia.domain.Fixture;

import java.time.Instant;

public record FixtureResponse(
        Long id,
        String competitionCode,
        Instant utcKickoff,
        String status,
        Integer matchday,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        Integer homeScore,
        Integer awayScore,
        String homeFormation,
        String awayFormation) {

    public static FixtureResponse from(Fixture f, String competitionCode, TeamResponse homeTeam, TeamResponse awayTeam) {
        return new FixtureResponse(
                f.getId(),
                competitionCode,
                f.getUtcKickoff(),
                f.getStatus().name(),
                f.getMatchday(),
                homeTeam,
                awayTeam,
                f.getHomeScore(),
                f.getAwayScore(),
                f.getHomeFormation(),
                f.getAwayFormation());
    }
}

package com.footpedia.web.dto;

import com.footpedia.domain.Standing;

public record StandingResponse(
        Integer position,
        TeamResponse team,
        Integer played,
        Integer won,
        Integer draw,
        Integer lost,
        Integer points,
        Integer goalsFor,
        Integer goalsAgainst,
        String form) {

    public static StandingResponse from(Standing s, TeamResponse team) {
        return new StandingResponse(
                s.getPosition(),
                team,
                s.getPlayed(),
                s.getWon(),
                s.getDraw(),
                s.getLost(),
                s.getPoints(),
                s.getGoalsFor(),
                s.getGoalsAgainst(),
                s.getForm());
    }
}

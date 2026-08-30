package com.footpedia.providers.footballdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StandingsResponse(List<StandingGroup> standings) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingGroup(String type, List<TableRowDto> table) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TableRowDto(
            Integer position,
            MatchDto.TeamRef team,
            Integer playedGames,
            Integer won,
            Integer draw,
            Integer lost,
            Integer points,
            Integer goalsFor,
            Integer goalsAgainst,
            String form) {
    }
}

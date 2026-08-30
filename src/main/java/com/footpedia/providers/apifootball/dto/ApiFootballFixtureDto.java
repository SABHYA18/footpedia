package com.footpedia.providers.apifootball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiFootballFixtureDto(FixtureInfo fixture, League league, Teams teams, Goals goals) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FixtureInfo(long id, String date, Status status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(@JsonProperty("short") String shortStatus) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record League(int id, int season, String round) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Teams(TeamRef home, TeamRef away) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamRef(long id, String name, String logo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Goals(Integer home, Integer away) {
    }
}

package com.footpedia.providers.footballdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchDto(
        long id, String utcDate, String status, Integer matchday, TeamRef homeTeam, TeamRef awayTeam, Score score) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamRef(long id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Score(FullTime fullTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FullTime(Integer home, Integer away) {
    }
}

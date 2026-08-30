package com.footpedia.providers.apifootball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiFootballStatsResponse(List<TeamStats> response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamStats(TeamRef team, List<StatEntry> statistics) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamRef(long id, String name) {
    }

    // value is a number, a percentage string ("55%"), or null depending on stat type.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntry(String type, Object value) {
    }
}

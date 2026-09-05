package com.footpedia.providers.espn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EspnScoreboardResponse(List<EventDto> events) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventDto(String id, String date, String name, List<CompetitionDto> competitions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompetitionDto(String id, StatusDto status, List<CompetitorDto> competitors) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusDto(StatusTypeDto type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusTypeDto(String id, String name, String state, Boolean completed, String detail) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompetitorDto(String id, String homeAway, String score, TeamRefDto team) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamRefDto(String id, String displayName, String abbreviation) {
    }
}

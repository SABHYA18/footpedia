package com.footpedia.providers.footballdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompetitionDto(
        long id, String name, String code, Area area, String emblem, CurrentSeason currentSeason) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Area(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentSeason(String startDate, String endDate, Integer currentMatchday) {
    }
}

package com.footpedia.providers.espn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EspnSummaryResponse(List<RosterDto> rosters, List<KeyEventDto> keyEvents, BoxscoreDto boxscore) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RosterDto(
            String homeAway, EspnScoreboardResponse.TeamRefDto team, List<RosterEntryDto> roster, String formation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RosterEntryDto(
            Boolean starter,
            String jersey,
            AthleteDto athlete,
            PositionDto position,
            Boolean subbedIn,
            Boolean subbedOut) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AthleteDto(String id, String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PositionDto(String abbreviation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyEventDto(
            String id,
            EventTypeDto type,
            String text,
            PeriodDto period,
            ClockDto clock,
            EspnScoreboardResponse.TeamRefDto team,
            List<ParticipantDto> participants) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventTypeDto(String id, String text, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PeriodDto(Integer number) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClockDto(Double value, String displayValue) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParticipantDto(AthleteDto athlete) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BoxscoreDto(List<BoxscoreTeamDto> teams) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BoxscoreTeamDto(EspnScoreboardResponse.TeamRefDto team, List<StatEntryDto> statistics) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntryDto(String name, String displayValue, String label) {
    }
}

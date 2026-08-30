package com.footpedia.providers.apifootball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiFootballPlayersResponse(List<TeamPlayers> response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamPlayers(TeamRef team, List<PlayerEntry> players) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamRef(long id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerEntry(PlayerInfo player, List<PlayerStats> statistics) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerInfo(long id, String name, String photo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerStats(
            Games games, Shots shots, Goals goals, Passes passes, Tackles tackles, Duels duels, Cards cards) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Games(Integer minutes, String position, String rating, Boolean captain) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Shots(Integer total, Integer on) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Goals(Integer total, Integer assists) {
    }

    // accuracy is a plain-number string (e.g. "19"), not a "19%" string.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Passes(Integer total, String accuracy) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tackles(Integer total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Duels(Integer total, Integer won) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cards(Integer yellow, Integer red) {
    }
}

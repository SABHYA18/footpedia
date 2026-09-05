package com.footpedia.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record FixtureStatsResponse(
        List<TeamStatEntry> teamStats,
        List<PlayerStatEntry> playerStats,
        List<LineupEntry> lineups,
        List<MatchEventEntry> events) {

    public record TeamStatEntry(Long teamId, String statType, String statValue) {
    }

    public record PlayerStatEntry(
            Long teamId,
            Long playerId,
            String playerName,
            String photoUrl,
            String position,
            Integer minutes,
            BigDecimal rating,
            Integer goals,
            Integer assists,
            Integer shotsTotal,
            Integer shotsOn,
            Integer passesTotal,
            Integer passesAccuracy,
            Integer tackles,
            Integer duelsTotal,
            Integer duelsWon,
            Integer yellowCards,
            Integer redCards) {
    }

    public record LineupEntry(
            Long teamId,
            Long playerId,
            String playerName,
            String position,
            Integer jerseyNumber,
            boolean starter,
            boolean subbedIn,
            boolean subbedOut,
            Integer formationPlace) {
    }

    public record MatchEventEntry(
            Long teamId,
            Long playerId,
            String playerName,
            Long relatedPlayerId,
            String relatedPlayerName,
            String eventType,
            Integer minute,
            String detail) {
    }
}

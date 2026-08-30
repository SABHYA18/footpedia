package com.footpedia.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record FixtureStatsResponse(List<TeamStatEntry> teamStats, List<PlayerStatEntry> playerStats) {

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
}

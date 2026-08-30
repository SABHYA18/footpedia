package com.footpedia.preferences.dto;

import com.footpedia.domain.NotificationChannel;

import java.util.List;

public record PreferencesResponse(
        List<Long> favoriteCompetitionIds,
        List<Long> favoriteTeamIds,
        Integer defaultLeadTimeMinutes,
        NotificationChannel notificationChannel,
        String telegramHandle) {
}

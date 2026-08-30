package com.footpedia.preferences.dto;

import com.footpedia.domain.NotificationChannel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdatePreferencesRequest(
        @NotNull List<Long> favoriteCompetitionIds,
        @NotNull List<Long> favoriteTeamIds,
        @Min(1) Integer defaultLeadTimeMinutes,
        @NotNull NotificationChannel notificationChannel,
        String telegramHandle) {
}

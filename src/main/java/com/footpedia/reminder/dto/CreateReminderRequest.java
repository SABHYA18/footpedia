package com.footpedia.reminder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReminderRequest(@NotNull Long fixtureId, @Min(1) Integer leadTimeMinutes) {
}

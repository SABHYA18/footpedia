package com.footpedia.reminder.dto;

import com.footpedia.domain.Reminder;

import java.time.Instant;

public record ReminderResponse(
        Long id, Long fixtureId, Instant notifyAt, int leadTimeSeconds, String status, Instant createdAt) {

    public static ReminderResponse from(Reminder r) {
        return new ReminderResponse(
                r.getId(), r.getFixtureId(), r.getNotifyAt(), r.getLeadTimeSeconds(), r.getStatus().name(), r.getCreatedAt());
    }
}

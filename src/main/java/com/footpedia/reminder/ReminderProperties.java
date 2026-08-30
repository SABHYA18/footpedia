package com.footpedia.reminder;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "footpedia.reminders")
public record ReminderProperties(Duration defaultLeadTime, String zsetKey) {
}

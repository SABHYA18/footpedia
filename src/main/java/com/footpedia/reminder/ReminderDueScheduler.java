package com.footpedia.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderDueScheduler {

    private final ReminderZSet reminderZSet;
    private final ReminderDueProcessor processor;

    // Mirrors footpedia.reminders.poll-interval in application.yml.
    @Scheduled(fixedRate = 30_000)
    public void poll() {
        List<Long> due = reminderZSet.popDue(Instant.now());
        for (Long reminderId : due) {
            try {
                processor.processOne(reminderId);
            } catch (Exception e) {
                log.error("Failed to process due reminder {}", reminderId, e);
            }
        }
    }
}

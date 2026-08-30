package com.footpedia.reminder;

import com.footpedia.domain.Reminder;
import com.footpedia.domain.ReminderStatus;
import com.footpedia.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The `reminder` table is the durable record; the Redis ZSET is just a fast
 * due-time index over it. On boot, reload every still-pending reminder into
 * the ZSET so a Redis flush or restart never silently drops one -- a reminder
 * that already fired late is far worse than one the poller catches a tick
 * after startup.
 */
@Component
@Slf4j
@RequiredArgsConstructor
class ReminderRehydration {

    private final ReminderRepository reminderRepository;
    private final ReminderZSet reminderZSet;

    @EventListener(ApplicationReadyEvent.class)
    void rehydrate() {
        List<Reminder> pending = reminderRepository.findByStatus(ReminderStatus.PENDING);
        pending.forEach(r -> reminderZSet.schedule(r.getId(), r.getNotifyAt()));
        log.info("Rehydrated {} pending reminders into Redis", pending.size());
    }
}

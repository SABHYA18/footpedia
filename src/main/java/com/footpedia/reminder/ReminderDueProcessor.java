package com.footpedia.reminder;

import com.footpedia.domain.Reminder;
import com.footpedia.domain.ReminderStatus;
import com.footpedia.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Processes one due reminder in its own transaction, so a failure on one
 * reminder in a poll tick can't roll back the others already handled in the
 * same tick.
 */
@Component
@Slf4j
@RequiredArgsConstructor
class ReminderDueProcessor {

    private final ReminderRepository reminderRepository;
    private final ReminderNotifier notifier;

    @Transactional
    void processOne(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != ReminderStatus.PENDING) {
            // Already cancelled, already sent, or deleted since it was popped -- nothing to do.
            return;
        }

        notifier.notify(reminder);

        reminder.setStatus(ReminderStatus.SENT);
        reminder.setSentAt(Instant.now());
        reminderRepository.save(reminder);
    }
}

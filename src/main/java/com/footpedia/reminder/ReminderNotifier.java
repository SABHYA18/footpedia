package com.footpedia.reminder;

import com.footpedia.domain.Reminder;

/** Delivers a due reminder to the user. Swapped for a Telegram-backed impl once the bot exists. */
public interface ReminderNotifier {
    void notify(Reminder reminder);
}

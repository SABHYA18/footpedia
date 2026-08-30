package com.footpedia.reminder;

import com.footpedia.reminder.dto.CreateReminderRequest;
import com.footpedia.reminder.dto.ReminderResponse;

import java.util.List;

public interface ReminderService {

    ReminderResponse create(String userRef, CreateReminderRequest request);

    List<ReminderResponse> list(String userRef);

    void cancel(String userRef, Long reminderId);
}

package com.footpedia.reminder;

import com.footpedia.domain.AppUser;
import com.footpedia.reminder.dto.CreateReminderRequest;
import com.footpedia.reminder.dto.ReminderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@AuthenticationPrincipal AppUser user, @Valid @RequestBody CreateReminderRequest request) {
        return reminderService.create(userRef(user), request);
    }

    @GetMapping
    public List<ReminderResponse> list(@AuthenticationPrincipal AppUser user) {
        return reminderService.list(userRef(user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AppUser user, @PathVariable Long id) {
        reminderService.cancel(userRef(user), id);
    }

    private String userRef(AppUser user) {
        return String.valueOf(user.getId());
    }
}

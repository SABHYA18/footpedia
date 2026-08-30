package com.footpedia.reminder;

import com.footpedia.domain.AppUser;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.Reminder;
import com.footpedia.domain.ReminderStatus;
import com.footpedia.reminder.dto.CreateReminderRequest;
import com.footpedia.reminder.dto.ReminderResponse;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.ReminderRepository;
import com.footpedia.web.exception.BadRequestException;
import com.footpedia.web.exception.ConflictException;
import com.footpedia.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final FixtureRepository fixtureRepository;
    private final AppUserRepository appUserRepository;
    private final ReminderZSet reminderZSet;
    private final ReminderProperties properties;

    @Override
    @Transactional
    public ReminderResponse create(String userRef, CreateReminderRequest request) {
        Fixture fixture = fixtureRepository
                .findById(request.fixtureId())
                .orElseThrow(() -> new NotFoundException("Unknown fixture: " + request.fixtureId()));

        Instant now = Instant.now();
        if (fixture.getUtcKickoff().isBefore(now)) {
            throw new BadRequestException("Fixture has already kicked off");
        }

        int leadTimeSeconds = request.leadTimeMinutes() != null
                ? request.leadTimeMinutes() * 60
                : userDefaultLeadTimeSeconds(userRef);
        Instant notifyAt = fixture.getUtcKickoff().minusSeconds(leadTimeSeconds);

        Reminder reminder = reminderRepository
                .findByUserRefAndFixtureId(userRef, request.fixtureId())
                .orElseGet(Reminder::new);

        if (reminder.getId() != null && reminder.getStatus() == ReminderStatus.PENDING) {
            throw new ConflictException("A reminder already exists for this fixture");
        }

        reminder.setUserRef(userRef);
        reminder.setFixtureId(request.fixtureId());
        reminder.setLeadTimeSeconds(leadTimeSeconds);
        reminder.setNotifyAt(notifyAt);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setSentAt(null);
        if (reminder.getCreatedAt() == null) {
            reminder.setCreatedAt(now);
        }
        reminder = reminderRepository.save(reminder);

        reminderZSet.schedule(reminder.getId(), notifyAt);
        return ReminderResponse.from(reminder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> list(String userRef) {
        return reminderRepository.findByUserRefOrderByNotifyAtAsc(userRef).stream()
                .filter(r -> r.getStatus() != ReminderStatus.CANCELLED)
                .map(ReminderResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void cancel(String userRef, Long reminderId) {
        Reminder reminder = reminderRepository
                .findById(reminderId)
                .filter(r -> r.getUserRef().equals(userRef))
                .orElseThrow(() -> new NotFoundException("Unknown reminder: " + reminderId));

        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new ConflictException("Only a pending reminder can be cancelled");
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);
        reminderZSet.cancel(reminderId);
    }

    private int userDefaultLeadTimeSeconds(String userRef) {
        Integer userDefaultMinutes = appUserRepository
                .findById(Long.valueOf(userRef))
                .map(AppUser::getDefaultLeadTimeMinutes)
                .orElse(null);
        return userDefaultMinutes != null ? userDefaultMinutes * 60 : (int) properties.defaultLeadTime().toSeconds();
    }
}

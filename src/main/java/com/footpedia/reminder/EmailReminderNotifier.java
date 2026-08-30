package com.footpedia.reminder;

import com.footpedia.auth.EmailService;
import com.footpedia.domain.AppUser;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.Reminder;
import com.footpedia.domain.Team;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Delivers a due reminder by email. Every user's notificationChannel is
 * still routed here regardless of preference (EMAIL/TELEGRAM/BOTH) -- the
 * Telegram bot doesn't exist yet, so email is the only channel that can
 * actually reach anyone, matching what the profile page already tells users.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailReminderNotifier implements ReminderNotifier {

    private final AppUserRepository appUserRepository;
    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final EmailService emailService;

    @Override
    public void notify(Reminder reminder) {
        AppUser user = appUserRepository.findById(Long.valueOf(reminder.getUserRef())).orElse(null);
        Fixture fixture = fixtureRepository.findById(reminder.getFixtureId()).orElse(null);
        if (user == null || fixture == null) {
            log.warn(
                    "Skipping reminder {} -- user or fixture no longer exists (userRef={}, fixtureId={})",
                    reminder.getId(), reminder.getUserRef(), reminder.getFixtureId());
            return;
        }

        Team homeTeam = teamRepository.findById(fixture.getHomeTeamId()).orElse(null);
        Team awayTeam = teamRepository.findById(fixture.getAwayTeamId()).orElse(null);

        emailService.sendReminderEmail(
                user.getEmail(),
                homeTeam != null ? homeTeam.getName() : "Home",
                awayTeam != null ? awayTeam.getName() : "Away",
                fixture.getUtcKickoff());

        log.info("Sent reminder email to {} for fixture {}", user.getEmail(), fixture.getId());
    }
}

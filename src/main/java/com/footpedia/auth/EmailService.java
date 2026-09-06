package com.footpedia.auth;

import java.time.Instant;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);

    void sendPasswordChangeCode(String toEmail, String code);

    void sendReminderEmail(String toEmail, String homeTeamName, String awayTeamName, Instant utcKickoff);
}

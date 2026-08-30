package com.footpedia.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class EmailServiceImpl implements EmailService {

    // Plain UTC, spelled out -- the backend has no reliable per-user timezone to convert to.
    private static final DateTimeFormatter KICKOFF_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(toEmail);
        message.setSubject("Reset your footpedia password");
        message.setText("""
                Someone requested a password reset for this footpedia account.

                Reset your password: %s

                This link expires in %d minutes. If you didn't request this, ignore this email.
                """.formatted(resetLink, mailProperties.resetTokenExpiryMinutes()));
        mailSender.send(message);
    }

    @Override
    public void sendReminderEmail(String toEmail, String homeTeamName, String awayTeamName, Instant utcKickoff) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(toEmail);
        message.setSubject("Kickoff soon: %s vs %s".formatted(homeTeamName, awayTeamName));
        message.setText("""
                %s vs %s kicks off %s.

                -- footpedia
                """.formatted(homeTeamName, awayTeamName, KICKOFF_FORMAT.format(utcKickoff)));
        mailSender.send(message);
    }
}

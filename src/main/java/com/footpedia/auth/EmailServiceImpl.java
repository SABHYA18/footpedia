package com.footpedia.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sends mail via Resend's HTTPS API rather than direct SMTP. Railway (like
 * most PaaS platforms) blocks outbound SMTP on the standard ports to curb
 * spam, so a {@code JavaMailSender} connection to smtp.gmail.com:587 hung
 * until it timed out and never delivered -- confirmed live in production
 * ({@code MailConnectException: ... Connect timed out}). An HTTPS call has
 * no such restriction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class EmailServiceImpl implements EmailService {

    // Plain UTC, spelled out -- the backend has no reliable per-user timezone to convert to.
    private static final DateTimeFormatter KICKOFF_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final WebClient resendWebClient;
    private final MailProperties mailProperties;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        send(toEmail, "Reset your footpedia password", """
                Someone requested a password reset for this footpedia account.

                Reset your password: %s

                This link expires in %d minutes. If you didn't request this, ignore this email.
                """.formatted(resetLink, mailProperties.resetTokenExpiryMinutes()));
    }

    @Override
    public void sendPasswordChangeCode(String toEmail, String code) {
        send(toEmail, "Your footpedia password change code", """
                Use this code to confirm you want to change this footpedia account's password:

                %s

                This code expires in %d minutes. If you didn't request this, ignore this email --
                your password won't change without it.
                """.formatted(code, mailProperties.changeCodeExpiryMinutes()));
    }

    @Override
    public void sendReminderEmail(String toEmail, String homeTeamName, String awayTeamName, Instant utcKickoff) {
        send(toEmail, "Kickoff soon: %s vs %s".formatted(homeTeamName, awayTeamName), """
                %s vs %s kicks off %s.

                -- footpedia
                """.formatted(homeTeamName, awayTeamName, KICKOFF_FORMAT.format(utcKickoff)));
    }

    private void send(String toEmail, String subject, String text) {
        Map<String, Object> body = Map.of(
                "from", mailProperties.from(),
                "to", List.of(toEmail),
                "subject", subject,
                "text", text);

        FetchResult result = resendWebClient.post()
                .uri("/emails")
                .bodyValue(body)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(responseBody -> new FetchResult(response.statusCode().value(), responseBody)))
                .block();

        if (result.status() >= 400) {
            log.error("Resend returned {} sending to {}: {}", result.status(), toEmail, result.body());
            // MailSendException so existing callers' `catch (MailException e)` keep working
            // unchanged -- this is still "the email didn't send", just over a different transport.
            throw new MailSendException("Resend returned %d".formatted(result.status()));
        }
    }

    private record FetchResult(int status, String body) {
    }
}

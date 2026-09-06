package com.footpedia.auth;

import com.footpedia.auth.dto.PasswordResetConfirmRequest;
import com.footpedia.auth.dto.PasswordResetRequest;
import com.footpedia.domain.AppUser;
import com.footpedia.domain.PasswordResetToken;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.repository.PasswordResetTokenRepository;
import com.footpedia.web.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    // Deliberately NOT @Transactional -- issueAndSendToken's DB writes each
    // auto-commit individually via Spring Data, and the mail send below is a
    // slow, blocking SMTP call with no business holding a DB connection open
    // for its duration (same fix as AccountServiceImpl.requestPasswordChangeCode).
    @Override
    public void requestReset(PasswordResetRequest request) {
        appUserRepository.findByEmail(request.email()).ifPresentOrElse(
                this::issueAndSendToken,
                () -> log.debug("Password reset requested for unknown email"));
    }

    @Override
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        String tokenHash = hash(request.token());
        PasswordResetToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        AppUser user = appUserRepository
                .findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
    }

    private void issueAndSendToken(AppUser user) {
        tokenRepository.deleteUnusedByUserId(user.getId());

        String rawToken = generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(mailProperties.resetTokenExpiryMinutes() * 60));
        token.setCreatedAt(Instant.now());
        tokenRepository.save(token);

        String resetLink = UriComponentsBuilder.fromUriString(mailProperties.frontendUrl())
                .path("/reset-password")
                .queryParam("token", rawToken)
                .toUriString();

        // Swallow send failures rather than letting them propagate: an SMTP
        // hiccup shouldn't surface as a 500 that differs from the "unknown
        // email" response, which would leak whether the account exists.
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } catch (MailException e) {
            log.error("Failed to send password reset email", e);
        }
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

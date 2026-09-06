package com.footpedia.account;

import com.footpedia.account.dto.ChangePasswordRequest;
import com.footpedia.account.dto.DeleteAccountRequest;
import com.footpedia.account.dto.UpdateProfileRequest;
import com.footpedia.auth.EmailService;
import com.footpedia.auth.MailProperties;
import com.footpedia.auth.dto.UserResponse;
import com.footpedia.domain.AppUser;
import com.footpedia.domain.PasswordChangeCode;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.repository.PasswordChangeCodeRepository;
import com.footpedia.repository.PasswordResetTokenRepository;
import com.footpedia.repository.ReminderRepository;
import com.footpedia.repository.UserLeaguePreferenceRepository;
import com.footpedia.repository.UserTeamPreferenceRepository;
import com.footpedia.web.exception.BadRequestException;
import com.footpedia.web.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReminderRepository reminderRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordChangeCodeRepository passwordChangeCodeRepository;
    private final UserLeaguePreferenceRepository leaguePreferenceRepository;
    private final UserTeamPreferenceRepository teamPreferenceRepository;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        user.setDisplayName(request.displayName());
        appUserRepository.save(user);
        return UserResponse.from(user);
    }

    /**
     * Deliberately NOT {@code @Transactional} -- the DB writes below are each
     * auto-committed individually by Spring Data (every {@code JpaRepository}
     * method is itself transactional), and {@code emailService.send(...)} is a
     * slow, blocking SMTP call that has no business holding a DB connection
     * open for its duration. This is the same class of bug fixed earlier for
     * ESPN's summary fetch ({@code EspnLiveIngestionService}/
     * {@code EspnMatchDetailPersister}): a slow external call wrapped in
     * {@code @Transactional} ties up a pooled connection for as long as that
     * call takes, which under HikariCP's small pool can starve unrelated
     * requests.
     *
     * <p>Also does NOT swallow a send failure the way
     * {@link com.footpedia.auth.PasswordResetServiceImpl} does -- that swallow
     * is deliberate there, to avoid leaking whether an email address has an
     * account. There's no such concern here (the caller is already
     * authenticated as themselves), so a failure is surfaced as a real error
     * instead of a false "sent" response the user has no way to act on.
     */
    @Override
    public void requestPasswordChangeCode(Long userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();

        passwordChangeCodeRepository.deleteUnusedByUserId(userId);

        String code = generateCode();
        PasswordChangeCode entity = new PasswordChangeCode();
        entity.setUserId(userId);
        entity.setCodeHash(hash(code));
        entity.setExpiresAt(Instant.now().plusSeconds(mailProperties.changeCodeExpiryMinutes() * 60));
        entity.setCreatedAt(Instant.now());
        passwordChangeCodeRepository.save(entity);

        try {
            emailService.sendPasswordChangeCode(user.getEmail(), code);
        } catch (MailException e) {
            log.error("Failed to send password change code", e);
            throw new ServiceUnavailableException("Couldn't send the verification email right now. Please try again shortly.");
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            // Not a 401: the caller's *token* is valid and they ARE authenticated -- this is a
            // bad-input rejection of the confirmation password, and a 401 here would trip the
            // frontend's blanket "401 means the token is dead, log out" handler.
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from your current password");
        }

        PasswordChangeCode code = passwordChangeCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId)
                .filter(c -> c.getExpiresAt().isAfter(Instant.now()))
                .filter(c -> c.getCodeHash().equals(hash(request.code())))
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));
        code.setUsedAt(Instant.now());
        passwordChangeCodeRepository.save(code);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }

    private static String generateCode() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Password is incorrect");
        }

        // Delete children before the parent row -- no ON DELETE CASCADE on these FKs.
        reminderRepository.deleteByUserRef(String.valueOf(userId));
        passwordResetTokenRepository.deleteByUserId(userId);
        passwordChangeCodeRepository.deleteByUserId(userId);
        leaguePreferenceRepository.deleteByUserId(userId);
        teamPreferenceRepository.deleteByUserId(userId);
        appUserRepository.delete(user);
    }
}

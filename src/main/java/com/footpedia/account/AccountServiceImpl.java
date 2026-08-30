package com.footpedia.account;

import com.footpedia.account.dto.ChangePasswordRequest;
import com.footpedia.account.dto.DeleteAccountRequest;
import com.footpedia.account.dto.UpdateProfileRequest;
import com.footpedia.auth.dto.UserResponse;
import com.footpedia.domain.AppUser;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.repository.PasswordResetTokenRepository;
import com.footpedia.repository.ReminderRepository;
import com.footpedia.repository.UserLeaguePreferenceRepository;
import com.footpedia.repository.UserTeamPreferenceRepository;
import com.footpedia.web.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReminderRepository reminderRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserLeaguePreferenceRepository leaguePreferenceRepository;
    private final UserTeamPreferenceRepository teamPreferenceRepository;

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        user.setDisplayName(request.displayName());
        appUserRepository.save(user);
        return UserResponse.from(user);
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
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
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
        leaguePreferenceRepository.deleteByUserId(userId);
        teamPreferenceRepository.deleteByUserId(userId);
        appUserRepository.delete(user);
    }
}

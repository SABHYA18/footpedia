package com.footpedia.preferences;

import com.footpedia.domain.AppUser;
import com.footpedia.domain.NotificationChannel;
import com.footpedia.domain.UserLeaguePreference;
import com.footpedia.domain.UserTeamPreference;
import com.footpedia.preferences.dto.PreferencesResponse;
import com.footpedia.preferences.dto.UpdatePreferencesRequest;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.TeamRepository;
import com.footpedia.repository.UserLeaguePreferenceRepository;
import com.footpedia.repository.UserTeamPreferenceRepository;
import com.footpedia.web.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferencesServiceImpl implements PreferencesService {

    private final UserLeaguePreferenceRepository leaguePreferenceRepository;
    private final UserTeamPreferenceRepository teamPreferenceRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PreferencesResponse get(Long userId) {
        List<Long> competitionIds = leaguePreferenceRepository.findByUserId(userId).stream()
                .map(UserLeaguePreference::getCompetitionId)
                .toList();
        List<Long> teamIds = teamPreferenceRepository.findByUserId(userId).stream()
                .map(UserTeamPreference::getTeamId)
                .toList();
        AppUser user = appUserRepository.findById(userId).orElseThrow();

        return new PreferencesResponse(
                competitionIds, teamIds, user.getDefaultLeadTimeMinutes(), user.getNotificationChannel(), user.getTelegramHandle());
    }

    @Override
    @Transactional
    public PreferencesResponse update(Long userId, UpdatePreferencesRequest request) {
        List<Long> competitionIds = request.favoriteCompetitionIds().stream().distinct().toList();
        List<Long> teamIds = request.favoriteTeamIds().stream().distinct().toList();

        if (competitionRepository.findAllById(competitionIds).size() != competitionIds.size()) {
            throw new BadRequestException("One or more favoriteCompetitionIds are unknown");
        }
        if (teamRepository.findAllById(teamIds).size() != teamIds.size()) {
            throw new BadRequestException("One or more favoriteTeamIds are unknown");
        }
        boolean needsTelegramHandle = request.notificationChannel() != NotificationChannel.EMAIL;
        if (needsTelegramHandle && (request.telegramHandle() == null || request.telegramHandle().isBlank())) {
            throw new BadRequestException("telegramHandle is required for the selected notification channel");
        }

        leaguePreferenceRepository.deleteByUserId(userId);
        competitionIds.forEach(id -> {
            UserLeaguePreference pref = new UserLeaguePreference();
            pref.setUserId(userId);
            pref.setCompetitionId(id);
            leaguePreferenceRepository.save(pref);
        });

        teamPreferenceRepository.deleteByUserId(userId);
        teamIds.forEach(id -> {
            UserTeamPreference pref = new UserTeamPreference();
            pref.setUserId(userId);
            pref.setTeamId(id);
            teamPreferenceRepository.save(pref);
        });

        AppUser user = appUserRepository.findById(userId).orElseThrow();
        user.setDefaultLeadTimeMinutes(request.defaultLeadTimeMinutes());
        user.setNotificationChannel(request.notificationChannel());
        user.setTelegramHandle(request.telegramHandle());
        appUserRepository.save(user);

        return new PreferencesResponse(
                competitionIds, teamIds, user.getDefaultLeadTimeMinutes(), user.getNotificationChannel(), user.getTelegramHandle());
    }
}

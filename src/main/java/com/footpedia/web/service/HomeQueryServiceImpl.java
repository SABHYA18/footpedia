package com.footpedia.web.service;

import com.footpedia.domain.Competition;
import com.footpedia.domain.UserLeaguePreference;
import com.footpedia.domain.UserTeamPreference;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.UserLeaguePreferenceRepository;
import com.footpedia.repository.UserTeamPreferenceRepository;
import com.footpedia.web.dto.FixtureResponse;
import com.footpedia.web.dto.HomeResponse;
import com.footpedia.web.dto.StandingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeQueryServiceImpl implements HomeQueryService {

    // Only Premier League is ingested so far -- see the backend README's build plan.
    // Falls back to this when a user hasn't set any favorite leagues yet, so an
    // empty preferences state still shows something instead of a blank home page.
    private static final String DEFAULT_COMPETITION_CODE = "PL";
    private static final int UPCOMING_LIMIT = 5;

    private final UserLeaguePreferenceRepository leaguePreferenceRepository;
    private final UserTeamPreferenceRepository teamPreferenceRepository;
    private final CompetitionRepository competitionRepository;
    private final FixtureQueryService fixtureQueryService;
    private final StandingQueryService standingQueryService;

    @Override
    public HomeResponse getHome(Long userId) {
        List<Long> favoriteCompetitionIds = leaguePreferenceRepository.findByUserId(userId).stream()
                .map(UserLeaguePreference::getCompetitionId)
                .toList();
        List<Long> favoriteTeamIds = teamPreferenceRepository.findByUserId(userId).stream()
                .map(UserTeamPreference::getTeamId)
                .toList();

        List<Competition> competitions = favoriteCompetitionIds.isEmpty()
                ? competitionRepository.findByCode(DEFAULT_COMPETITION_CODE).map(List::of).orElse(List.of())
                : competitionRepository.findAllById(favoriteCompetitionIds);

        List<FixtureResponse> todaysFixtures = competitions.stream()
                .flatMap(c -> fixtureQueryService.getTodaysFixtures(c.getCode()).stream())
                .collect(Collectors.toList());

        List<FixtureResponse> upcomingForYourTeams = fixtureQueryService.getUpcomingForTeams(favoriteTeamIds, UPCOMING_LIMIT);

        List<StandingResponse> yourTeamsStandings = favoriteTeamIds.isEmpty()
                ? List.of()
                : competitions.stream()
                        .flatMap(c -> standingQueryService.getStandings(c.getCode()).stream())
                        .filter(s -> favoriteTeamIds.contains(s.team().id()))
                        .collect(Collectors.toList());

        return new HomeResponse(todaysFixtures, upcomingForYourTeams, yourTeamsStandings);
    }
}

package com.footpedia.web.service;

import com.footpedia.config.CacheNames;
import com.footpedia.domain.Competition;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.domain.Team;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.TeamRepository;
import com.footpedia.web.dto.FixtureResponse;
import com.footpedia.web.dto.TeamResponse;
import com.footpedia.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixtureQueryServiceImpl implements FixtureQueryService {

    private static final List<FixtureStatus> LIVE_STATUSES = List.of(FixtureStatus.IN_PLAY, FixtureStatus.PAUSED);

    private final CompetitionLookup competitionLookup;
    private final CompetitionRepository competitionRepository;
    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;

    @Override
    @Cacheable(cacheNames = CacheNames.FIXTURE_LIST, key = "#competitionCode + ':' + #season + ':' + #matchday")
    public List<FixtureResponse> getFixtures(String competitionCode, String season, Integer matchday) {
        Competition competition = competitionLookup.byCode(competitionCode);
        String effectiveSeason = season != null ? season : competition.getCurrentSeason();
        List<Fixture> fixtures = matchday == null
                ? fixtureRepository.findByCompetitionIdAndSeasonOrderByUtcKickoffAsc(competition.getId(), effectiveSeason)
                : fixtureRepository.findByCompetitionIdAndSeasonAndMatchdayOrderByUtcKickoffAsc(
                        competition.getId(), effectiveSeason, matchday);
        return toResponses(fixtures);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.TODAYS_FIXTURES, key = "#competitionCode + ':' + T(java.time.LocalDate).now()")
    public List<FixtureResponse> getTodaysFixtures(String competitionCode) {
        Competition competition = competitionLookup.byCode(competitionCode);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return toResponses(fixtureRepository.findByCompetitionIdAndSeasonAndUtcKickoffBetweenOrderByUtcKickoffAsc(
                competition.getId(), competition.getCurrentSeason(), from, to));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.LIVE_SCORE, key = "#competitionCode")
    public List<FixtureResponse> getLiveFixtures(String competitionCode) {
        Competition competition = competitionLookup.byCode(competitionCode);
        return toResponses(fixtureRepository.findByCompetitionIdAndSeasonAndStatusInOrderByUtcKickoffAsc(
                competition.getId(), competition.getCurrentSeason(), LIVE_STATUSES));
    }

    @Override
    public List<FixtureResponse> getUpcomingForTeams(List<Long> teamIds, int limit) {
        if (teamIds.isEmpty()) {
            return List.of();
        }
        List<Fixture> fixtures = fixtureRepository.findUpcomingForTeams(teamIds, Instant.now(), PageRequest.of(0, limit));
        return toResponses(fixtures);
    }

    @Override
    public FixtureResponse getById(Long id) {
        Fixture fixture = fixtureRepository.findById(id).orElseThrow(() -> new NotFoundException("Unknown fixture: " + id));
        Map<Long, Team> teamsById = teamsById(List.of(fixture));
        Map<Long, Competition> competitionsById = competitionsById(List.of(fixture));
        return FixtureResponse.from(
                fixture,
                competitionsById.get(fixture.getCompetitionId()).getCode(),
                TeamResponse.from(teamsById.get(fixture.getHomeTeamId())),
                TeamResponse.from(teamsById.get(fixture.getAwayTeamId())));
    }

    private List<FixtureResponse> toResponses(List<Fixture> fixtures) {
        Map<Long, Team> teamsById = teamsById(fixtures);
        Map<Long, Competition> competitionsById = competitionsById(fixtures);
        // Collectors.toList() (a plain ArrayList), not Stream.toList() -- the
        // latter's private ImmutableCollections$ListN doesn't round-trip
        // through GenericJackson2JsonRedisSerializer's default typing.
        return fixtures.stream()
                .map(f -> FixtureResponse.from(
                        f,
                        competitionsById.get(f.getCompetitionId()).getCode(),
                        TeamResponse.from(teamsById.get(f.getHomeTeamId())),
                        TeamResponse.from(teamsById.get(f.getAwayTeamId()))))
                .collect(Collectors.toList());
    }

    private Map<Long, Competition> competitionsById(List<Fixture> fixtures) {
        List<Long> competitionIds = fixtures.stream().map(Fixture::getCompetitionId).distinct().toList();
        return competitionRepository.findAllById(competitionIds).stream()
                .collect(Collectors.toMap(Competition::getId, Function.identity()));
    }

    private Map<Long, Team> teamsById(List<Fixture> fixtures) {
        List<Long> teamIds = fixtures.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .distinct()
                .toList();
        return teamRepository.findAllById(teamIds).stream().collect(Collectors.toMap(Team::getId, Function.identity()));
    }
}

package com.footpedia.web.service;

import com.footpedia.config.CacheNames;
import com.footpedia.domain.Competition;
import com.footpedia.domain.Standing;
import com.footpedia.domain.Team;
import com.footpedia.repository.StandingRepository;
import com.footpedia.repository.TeamRepository;
import com.footpedia.web.dto.StandingResponse;
import com.footpedia.web.dto.TeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingQueryServiceImpl implements StandingQueryService {

    private final CompetitionLookup competitionLookup;
    private final StandingRepository standingRepository;
    private final TeamRepository teamRepository;

    @Override
    @Cacheable(cacheNames = CacheNames.STANDINGS, key = "#competitionCode")
    public List<StandingResponse> getStandings(String competitionCode) {
        Competition competition = competitionLookup.byCode(competitionCode);
        List<Standing> standings = standingRepository.findByCompetitionIdAndSeasonOrderByPositionAsc(
                competition.getId(), competition.getCurrentSeason());

        Map<Long, Team> teamsById = teamRepository
                .findAllById(standings.stream().map(Standing::getTeamId).toList())
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        // Collectors.toList() (a plain ArrayList), not Stream.toList() -- the
        // latter's private ImmutableCollections$ListN doesn't round-trip
        // through GenericJackson2JsonRedisSerializer's default typing.
        return standings.stream()
                .map(s -> StandingResponse.from(s, TeamResponse.from(teamsById.get(s.getTeamId()))))
                .collect(Collectors.toList());
    }
}

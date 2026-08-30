package com.footpedia.web.service;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Standing;
import com.footpedia.repository.StandingRepository;
import com.footpedia.repository.TeamRepository;
import com.footpedia.web.dto.TeamResponse;
import com.footpedia.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamQueryServiceImpl implements TeamQueryService {

    private final CompetitionLookup competitionLookup;
    private final TeamRepository teamRepository;
    private final StandingRepository standingRepository;

    @Override
    public TeamResponse getById(Long id) {
        return teamRepository
                .findById(id)
                .map(TeamResponse::from)
                .orElseThrow(() -> new NotFoundException("Unknown team: " + id));
    }

    @Override
    public List<TeamResponse> getByCompetition(String competitionCode) {
        Competition competition = competitionLookup.byCode(competitionCode);
        List<Long> teamIds = standingRepository
                .findByCompetitionIdAndSeasonOrderByPositionAsc(competition.getId(), competition.getCurrentSeason())
                .stream()
                .map(Standing::getTeamId)
                .toList();

        return teamRepository.findAllById(teamIds).stream().map(TeamResponse::from).toList();
    }
}

package com.footpedia.web.service;

import com.footpedia.web.dto.PlayerResponse;
import com.footpedia.web.dto.TeamResponse;

import java.util.List;

public interface TeamQueryService {
    TeamResponse getById(Long id);

    List<TeamResponse> getByCompetition(String competitionCode);

    /** Current squad, derived from Player.currentTeamId -- whoever this team's ingested lineups/stats have most recently attributed to it. */
    List<PlayerResponse> getPlayersForTeam(Long teamId);
}

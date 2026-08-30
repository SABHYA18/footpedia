package com.footpedia.web.service;

import com.footpedia.web.dto.TeamResponse;

import java.util.List;

public interface TeamQueryService {
    TeamResponse getById(Long id);

    List<TeamResponse> getByCompetition(String competitionCode);
}

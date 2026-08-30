package com.footpedia.web.service;

import com.footpedia.web.dto.StandingResponse;

import java.util.List;

public interface StandingQueryService {
    List<StandingResponse> getStandings(String competitionCode);
}

package com.footpedia.web.service;

import com.footpedia.web.dto.FixtureResponse;

import java.util.List;

public interface FixtureQueryService {

    List<FixtureResponse> getFixtures(String competitionCode, String season, Integer matchday);

    List<FixtureResponse> getTodaysFixtures(String competitionCode);

    List<FixtureResponse> getLiveFixtures(String competitionCode);

    List<FixtureResponse> getUpcomingForTeams(List<Long> teamIds, int limit);

    FixtureResponse getById(Long id);

    /** Most recent finished fixtures for a team (any competition, most recent first), up to {@code limit}. */
    List<FixtureResponse> getPreviousFixturesForTeam(Long teamId, int limit);
}

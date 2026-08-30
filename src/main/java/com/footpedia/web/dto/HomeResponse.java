package com.footpedia.web.dto;

import java.util.List;

public record HomeResponse(
        List<FixtureResponse> todaysFixtures,
        List<FixtureResponse> upcomingForYourTeams,
        List<StandingResponse> yourTeamsStandings) {}

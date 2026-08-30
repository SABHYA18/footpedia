package com.footpedia.web.service;

import com.footpedia.web.dto.FixtureStatsResponse;

public interface FixtureStatsQueryService {
    FixtureStatsResponse getStats(Long fixtureId);
}

package com.footpedia.web;

import com.footpedia.web.dto.FixtureResponse;
import com.footpedia.web.dto.FixtureStatsResponse;
import com.footpedia.web.service.FixtureQueryService;
import com.footpedia.web.service.FixtureStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fixtures")
@RequiredArgsConstructor
public class FixtureController {

    private final FixtureQueryService fixtureQueryService;
    private final FixtureStatsQueryService fixtureStatsQueryService;

    @GetMapping("/{id}")
    public FixtureResponse get(@PathVariable Long id) {
        return fixtureQueryService.getById(id);
    }

    @GetMapping("/{id}/stats")
    public FixtureStatsResponse stats(@PathVariable Long id) {
        return fixtureStatsQueryService.getStats(id);
    }
}

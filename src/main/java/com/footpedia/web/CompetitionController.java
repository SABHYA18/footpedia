package com.footpedia.web;

import com.footpedia.web.dto.CompetitionResponse;
import com.footpedia.web.dto.FixtureResponse;
import com.footpedia.web.dto.StandingResponse;
import com.footpedia.web.dto.TeamResponse;
import com.footpedia.web.service.CompetitionQueryService;
import com.footpedia.web.service.FixtureQueryService;
import com.footpedia.web.service.StandingQueryService;
import com.footpedia.web.service.TeamQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionQueryService competitionQueryService;
    private final TeamQueryService teamQueryService;
    private final StandingQueryService standingQueryService;
    private final FixtureQueryService fixtureQueryService;

    @GetMapping("/{code}")
    public CompetitionResponse get(@PathVariable String code) {
        return competitionQueryService.getByCode(code);
    }

    @GetMapping("/{code}/teams")
    public List<TeamResponse> teams(@PathVariable String code) {
        return teamQueryService.getByCompetition(code);
    }

    @GetMapping("/{code}/standings")
    public List<StandingResponse> standings(@PathVariable String code) {
        return standingQueryService.getStandings(code);
    }

    @GetMapping("/{code}/fixtures")
    public List<FixtureResponse> fixtures(
            @PathVariable String code,
            @RequestParam(required = false) Integer matchday,
            @RequestParam(required = false) String season) {
        return fixtureQueryService.getFixtures(code, season, matchday);
    }

    @GetMapping("/{code}/fixtures/today")
    public List<FixtureResponse> todaysFixtures(@PathVariable String code) {
        return fixtureQueryService.getTodaysFixtures(code);
    }

    @GetMapping("/{code}/fixtures/live")
    public List<FixtureResponse> liveFixtures(@PathVariable String code) {
        return fixtureQueryService.getLiveFixtures(code);
    }
}

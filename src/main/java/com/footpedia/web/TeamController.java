package com.footpedia.web;

import com.footpedia.web.dto.FixtureResponse;
import com.footpedia.web.dto.TeamResponse;
import com.footpedia.web.service.FixtureQueryService;
import com.footpedia.web.service.TeamQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamQueryService teamQueryService;
    private final FixtureQueryService fixtureQueryService;

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable Long id) {
        return teamQueryService.getById(id);
    }

    /** Null (empty body) if the team hasn't played a finished fixture yet. */
    @GetMapping("/{id}/previous-fixture")
    public FixtureResponse previousFixture(@PathVariable Long id) {
        return fixtureQueryService.getPreviousFixtureForTeam(id);
    }
}

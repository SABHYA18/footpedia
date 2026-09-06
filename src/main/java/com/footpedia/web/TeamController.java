package com.footpedia.web;

import com.footpedia.web.dto.FixtureResponse;
import com.footpedia.web.dto.PlayerResponse;
import com.footpedia.web.dto.TeamResponse;
import com.footpedia.web.service.FixtureQueryService;
import com.footpedia.web.service.TeamQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private static final int DEFAULT_PREVIOUS_FIXTURES = 5;

    private final TeamQueryService teamQueryService;
    private final FixtureQueryService fixtureQueryService;

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable Long id) {
        return teamQueryService.getById(id);
    }

    /** Most recent finished fixtures, any competition (domestic league + Champions League alike), most recent first. */
    @GetMapping("/{id}/previous-fixtures")
    public List<FixtureResponse> previousFixtures(
            @PathVariable Long id, @RequestParam(defaultValue = "" + DEFAULT_PREVIOUS_FIXTURES) int limit) {
        return fixtureQueryService.getPreviousFixturesForTeam(id, limit);
    }

    @GetMapping("/{id}/players")
    public List<PlayerResponse> players(@PathVariable Long id) {
        return teamQueryService.getPlayersForTeam(id);
    }
}

package com.footpedia.web;

import com.footpedia.web.dto.TeamResponse;
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

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable Long id) {
        return teamQueryService.getById(id);
    }
}

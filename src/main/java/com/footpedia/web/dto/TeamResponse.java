package com.footpedia.web.dto;

import com.footpedia.domain.Team;

public record TeamResponse(
        Long id, String name, String shortName, String tla, String crestUrl, String venue, Integer founded) {

    public static TeamResponse from(Team t) {
        return new TeamResponse(t.getId(), t.getName(), t.getShortName(), t.getTla(), t.getCrestUrl(), t.getVenue(), t.getFounded());
    }
}

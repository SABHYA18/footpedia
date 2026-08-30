package com.footpedia.web.dto;

import com.footpedia.domain.Competition;

public record CompetitionResponse(
        Long id, String code, String name, String country, String currentSeason, String emblemUrl) {

    public static CompetitionResponse from(Competition c) {
        return new CompetitionResponse(c.getId(), c.getCode(), c.getName(), c.getCountry(), c.getCurrentSeason(), c.getEmblemUrl());
    }
}

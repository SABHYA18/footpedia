package com.footpedia.providers.apifootball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiFootballFixtureListResponse(List<ApiFootballFixtureDto> response) {
}

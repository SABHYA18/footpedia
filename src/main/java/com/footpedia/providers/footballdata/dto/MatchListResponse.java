package com.footpedia.providers.footballdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchListResponse(List<MatchDto> matches) {
}

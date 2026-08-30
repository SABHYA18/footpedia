package com.footpedia.providers.footballdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamDto(
        long id, String name, String shortName, String tla, String crest, String venue, Integer founded) {
}

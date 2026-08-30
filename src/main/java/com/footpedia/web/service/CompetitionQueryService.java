package com.footpedia.web.service;

import com.footpedia.web.dto.CompetitionResponse;

public interface CompetitionQueryService {
    CompetitionResponse getByCode(String code);
}

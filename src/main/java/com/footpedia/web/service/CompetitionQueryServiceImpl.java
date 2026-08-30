package com.footpedia.web.service;

import com.footpedia.web.dto.CompetitionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionQueryServiceImpl implements CompetitionQueryService {

    private final CompetitionLookup competitionLookup;

    @Override
    public CompetitionResponse getByCode(String code) {
        return CompetitionResponse.from(competitionLookup.byCode(code));
    }
}

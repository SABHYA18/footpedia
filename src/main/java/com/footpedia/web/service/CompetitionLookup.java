package com.footpedia.web.service;

import com.footpedia.domain.Competition;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Shared "find competition by code or 404" used by every query service below. */
@Component
@RequiredArgsConstructor
class CompetitionLookup {

    private final CompetitionRepository competitionRepository;

    Competition byCode(String code) {
        return competitionRepository
                .findByCode(code)
                .orElseThrow(() -> new NotFoundException("Unknown competition: " + code));
    }
}

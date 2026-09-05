package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Polls ESPN's scoreboard every 30s (matching the live-score cache TTL) for
 * competitions with a fixture today, updating score/status faster than
 * football-data.org's 5-min cycle. Skips a competition entirely on days
 * with no fixture -- that local check is free, so this never spends an
 * ESPN call for nothing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EspnLiveScoreScheduler {

    private static final List<String> COMPETITION_CODES = List.of("PL", "PD", "SA", "BL1", "CL");

    private final EspnLiveIngestionService ingestionService;
    private final CompetitionRepository competitionRepository;
    private final FixtureRepository fixtureRepository;

    @Scheduled(initialDelay = 45_000, fixedRate = 30_000)
    public void syncLiveScores() {
        Instant windowStart = Instant.now().minus(Duration.ofHours(6));
        Instant windowEnd = Instant.now().plus(Duration.ofHours(18));

        for (String code : COMPETITION_CODES) {
            String slug = EspnLeagueSlugs.slugFor(code);
            Competition competition = competitionRepository.findByCode(code).orElse(null);
            if (competition == null || slug == null || competition.getCurrentSeason() == null) {
                continue;
            }

            boolean hasFixtureToday = !fixtureRepository
                    .findByCompetitionIdAndSeasonAndUtcKickoffBetweenOrderByUtcKickoffAsc(
                            competition.getId(), competition.getCurrentSeason(), windowStart, windowEnd)
                    .isEmpty();
            if (!hasFixtureToday) {
                continue;
            }

            try {
                ingestionService.syncScores(competition, slug);
            } catch (Exception e) {
                log.error("ESPN live score sync failed for {}", code, e);
            }
        }
    }
}

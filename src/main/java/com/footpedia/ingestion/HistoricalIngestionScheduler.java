package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Advances the historical (API-Football free-plan) backfill one matchday at
 * a time, working through {@link #SEASONS} in order -- 2023-24 first, then
 * 2024-25 (2025-26 and later are blocked on the free plan; 2022-23 was left
 * out since 2023-24 already covers the demo need). Progress is derived from
 * the data itself -- the highest matchday already stored for a given season
 * -- rather than a separate tracking table, since {@link HistoricalIngestionService}
 * upserts are already idempotent and the fixture table is the source of truth.
 *
 * <p>Four runs/day (~21 calls each, ~84/day total) comfortably fits the
 * 100/day budget with the configured reserve intact, and finishes both
 * 38-matchday seasons in about 19 days instead of 76.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HistoricalIngestionScheduler {

    private static final String COMPETITION_CODE = "PL";
    private static final int LEAGUE_ID = 39;
    private static final List<Integer> SEASONS = List.of(2023, 2024);
    private static final int LAST_MATCHDAY = 38;

    private final HistoricalIngestionService historicalIngestionService;
    private final FixtureRepository fixtureRepository;
    private final CompetitionRepository competitionRepository;

    @Scheduled(cron = "0 0 */6 * * *")
    public void ingestNextMatchday() {
        Competition competition = competitionRepository.findByCode(COMPETITION_CODE).orElse(null);
        if (competition == null) {
            log.warn("Historical backfill skipped: competition {} not yet synced", COMPETITION_CODE);
            return;
        }

        for (int season : SEASONS) {
            int lastIngested = fixtureRepository.findMaxMatchday(competition.getId(), String.valueOf(season)).orElse(0);
            if (lastIngested >= LAST_MATCHDAY) {
                continue; // this season's done, move on to the next
            }

            int nextMatchday = lastIngested + 1;
            try {
                HistoricalIngestSummary summary =
                        historicalIngestionService.ingestMatchday(COMPETITION_CODE, LEAGUE_ID, season, nextMatchday);
                log.info("Scheduled historical backfill: season {} matchday {} -> {}", season, nextMatchday, summary);
            } catch (Exception e) {
                log.error("Scheduled historical backfill failed for season {} matchday {}", season, nextMatchday, e);
            }
            return; // one matchday per run
        }

        log.info("Historical backfill complete for all configured seasons: {}", SEASONS);
    }
}

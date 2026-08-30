package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Week 1 scope is Premier League only. Teams/competition metadata rarely
 * change, so they sync hourly; fixtures/standings sync every 5 minutes to
 * match the {@code todays-fixtures} cache TTL.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PremierLeagueIngestionScheduler {

    private static final String COMPETITION_CODE = "PL";

    private final FootballDataIngestionService ingestionService;
    private final CompetitionRepository competitionRepository;

    @Scheduled(initialDelay = 0, fixedRate = 60 * 60 * 1000)
    public void syncCompetitionAndTeams() {
        try {
            Competition competition = ingestionService.syncCompetition(COMPETITION_CODE);
            ingestionService.syncTeams(competition, COMPETITION_CODE);
        } catch (Exception e) {
            log.error("Premier League competition/team sync failed", e);
        }
    }

    /**
     * Reads the competition already synced hourly rather than re-fetching it
     * from football-data.org every cycle -- that metadata almost never
     * changes mid-season, so spending an API call on it here just burns
     * budget and adds a request the free tier's rate limiting can flake on.
     */
    @Scheduled(initialDelay = 30_000, fixedRate = 5 * 60 * 1000)
    public void syncFixturesAndStandings() {
        try {
            Competition competition = competitionRepository.findByCode(COMPETITION_CODE).orElse(null);
            if (competition == null) {
                log.warn("Competition {} not yet synced, skipping fixtures/standings this cycle", COMPETITION_CODE);
                return;
            }
            ingestionService.syncFixtures(competition, COMPETITION_CODE);
            ingestionService.syncStandings(competition, COMPETITION_CODE);
        } catch (Exception e) {
            log.error("Premier League fixture/standings sync failed", e);
        }
    }
}

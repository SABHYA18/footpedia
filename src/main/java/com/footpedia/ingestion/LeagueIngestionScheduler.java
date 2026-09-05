package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Premier League, La Liga, Serie A, Bundesliga and the Champions League --
 * football-data.org's free tier covers all five and polling there has no
 * daily cap, so every competition rides the same schedule. Team/competition
 * metadata rarely changes, so it syncs hourly; fixtures/standings sync every
 * 5 minutes to match the {@code todays-fixtures} cache TTL.
 *
 * <p>The Champions League's Swiss league-phase table comes back from the
 * standings endpoint as a single {@code TOTAL} group, same shape as a
 * domestic league table, so no special-casing is needed here.
 *
 * <p>Each cycle makes 2 calls per competition (10 for all five), which is
 * exactly the football-data rate limiter's 10-per-minute budget -- see the
 * bumped {@code timeout-duration} in application.yml so a full cycle queues
 * across the per-minute window instead of erroring when it doesn't all fit
 * in one window's 10 permits.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LeagueIngestionScheduler {

    private static final List<String> COMPETITION_CODES = List.of("PL", "PD", "SA", "BL1", "CL");

    private final FootballDataIngestionService ingestionService;
    private final CompetitionRepository competitionRepository;

    @Scheduled(initialDelay = 0, fixedRate = 60 * 60 * 1000)
    public void syncCompetitionsAndTeams() {
        for (String code : COMPETITION_CODES) {
            try {
                Competition competition = ingestionService.syncCompetition(code);
                ingestionService.syncTeams(competition, code);
            } catch (Exception e) {
                log.error("Competition/team sync failed for {}", code, e);
            }
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
        for (String code : COMPETITION_CODES) {
            Competition competition = competitionRepository.findByCode(code).orElse(null);
            if (competition == null) {
                log.warn("Competition {} not yet synced, skipping fixtures/standings this cycle", code);
                continue;
            }
            try {
                ingestionService.syncFixtures(competition, code);
                ingestionService.syncStandings(competition, code);
            } catch (Exception e) {
                log.error("Fixture/standings sync failed for {}", code, e);
            }
        }
    }
}

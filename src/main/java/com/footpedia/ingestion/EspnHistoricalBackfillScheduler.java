package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Backfills lineups/events/team-stats for already-finished fixtures that
 * {@link EspnMatchDetailScheduler}'s 3-hour lookback already missed --
 * anything from yesterday's full matchday back to the start of the current
 * season. Unlike the API-Football historical backfill (100/day, PL-only,
 * walks one matchday per run), ESPN has no daily cap, so this just finds
 * every current-season fixture still missing detail and works through it.
 *
 * <p>Progress is derived from the data itself ({@code detailFetchedAt IS
 * NULL}), same reasoning as the API-Football scheduler: no separate
 * tracking table, and a fixture that already has detail is never re-fetched.
 *
 * <p>Scoped to the current season only -- older seasons (2023-24, 2024-25)
 * are API-Football's job via {@link HistoricalIngestionService}, which
 * pulls deeper per-player stats (passes, tackles, duels) ESPN doesn't
 * reliably expose.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EspnHistoricalBackfillScheduler {

    private static final List<String> COMPETITION_CODES = List.of("PL", "PD", "SA", "BL1", "CL");

    private final EspnLiveIngestionService ingestionService;
    private final CompetitionRepository competitionRepository;
    private final FixtureRepository fixtureRepository;

    @Scheduled(cron = "0 */15 * * * *")
    public void backfillMissingDetail() {
        for (String code : COMPETITION_CODES) {
            String slug = EspnLeagueSlugs.slugFor(code);
            Competition competition = competitionRepository.findByCode(code).orElse(null);
            if (competition == null || slug == null || competition.getCurrentSeason() == null) {
                continue;
            }
            try {
                backfillCompetition(competition, code, slug);
            } catch (Exception e) {
                log.error("ESPN historical backfill failed for {}", code, e);
            }
        }
    }

    private void backfillCompetition(Competition competition, String code, String slug) {
        List<Fixture> missing = fixtureRepository.findByCompetitionIdAndSeasonAndStatusAndDetailFetchedAtIsNull(
                competition.getId(), competition.getCurrentSeason(), FixtureStatus.FINISHED);
        if (missing.isEmpty()) {
            return;
        }

        // One scoreboard call covers every fixture on that date, so group by
        // date first rather than looking up each fixture individually.
        Set<LocalDate> dates = new TreeSet<>();
        for (Fixture fixture : missing) {
            dates.add(LocalDate.ofInstant(fixture.getUtcKickoff(), ZoneOffset.UTC));
        }

        log.info("ESPN historical backfill: {} fixtures missing detail across {} dates for {}", missing.size(), dates.size(), code);

        for (LocalDate date : dates) {
            try {
                ingestionService.syncScores(competition, slug, date);
            } catch (Exception e) {
                log.error("ESPN historical scoreboard sync failed for {} on {}", code, date, e);
            }
        }

        // Re-query: syncScores may have just linked external_refs for
        // fixtures that weren't previously matched to an ESPN event.
        for (Fixture fixture : fixtureRepository.findByCompetitionIdAndSeasonAndStatusAndDetailFetchedAtIsNull(
                competition.getId(), competition.getCurrentSeason(), FixtureStatus.FINISHED)) {
            try {
                ingestionService.syncMatchDetail(fixture, slug);
            } catch (Exception e) {
                log.error("ESPN historical detail sync failed for fixture {}", fixture.getId(), e);
            }
        }
    }
}

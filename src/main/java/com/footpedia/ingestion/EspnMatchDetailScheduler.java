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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Lineups/events/team-stats come from a much heavier ESPN payload (~400KB+
 * per match) than the scoreboard, so this runs on its own slower cadence:
 * every 2 minutes for matches in play, plus one final pass shortly after
 * full time to lock in the finished state. {@code detailFetchedAt} marks
 * that final pass done, so a fixture already finalized isn't re-fetched for
 * the rest of the 3-hour lookback window.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EspnMatchDetailScheduler {

    private static final List<String> COMPETITION_CODES = List.of("PL", "PD", "SA", "BL1", "CL");
    private static final List<FixtureStatus> LIVE_STATUSES = List.of(FixtureStatus.IN_PLAY, FixtureStatus.PAUSED);
    private static final Duration FINISHED_LOOKBACK = Duration.ofHours(3);

    private final EspnLiveIngestionService ingestionService;
    private final CompetitionRepository competitionRepository;
    private final FixtureRepository fixtureRepository;

    @Scheduled(initialDelay = 60_000, fixedRate = 2 * 60 * 1000)
    public void syncMatchDetail() {
        for (String code : COMPETITION_CODES) {
            String slug = EspnLeagueSlugs.slugFor(code);
            Competition competition = competitionRepository.findByCode(code).orElse(null);
            if (competition == null || slug == null || competition.getCurrentSeason() == null) {
                continue;
            }

            fixtureRepository
                    .findByCompetitionIdAndSeasonAndStatusInOrderByUtcKickoffAsc(
                            competition.getId(), competition.getCurrentSeason(), LIVE_STATUSES)
                    .forEach(fixture -> syncOne(fixture, slug));

            fixtureRepository
                    .findByCompetitionIdAndSeasonAndUtcKickoffBetweenOrderByUtcKickoffAsc(
                            competition.getId(), competition.getCurrentSeason(),
                            Instant.now().minus(FINISHED_LOOKBACK), Instant.now())
                    .stream()
                    .filter(f -> f.getStatus() == FixtureStatus.FINISHED && f.getDetailFetchedAt() == null)
                    .forEach(fixture -> syncOne(fixture, slug));
        }
    }

    private void syncOne(Fixture fixture, String slug) {
        try {
            ingestionService.syncMatchDetail(fixture, slug);
        } catch (Exception e) {
            log.error("ESPN match detail sync failed for fixture {}", fixture.getId(), e);
        }
    }
}

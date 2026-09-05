package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.providers.espn.EspnClient;
import com.footpedia.providers.espn.EspnException;
import com.footpedia.providers.espn.dto.EspnScoreboardResponse;
import com.footpedia.providers.espn.dto.EspnSummaryResponse;
import com.footpedia.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * ESPN's unofficial site API (undocumented, no key, no published rate limit)
 * fills the gap football-data.org's free tier leaves for the live season:
 * faster status/score updates than its 5-min cycle, plus lineups, match
 * events (goals/cards/subs) and richer team stats. It supplements rather
 * than replaces the existing providers -- standings and the season fixture
 * list still come from football-data.org.
 *
 * <p>Verified against a live response before building this: ESPN does not
 * reliably expose a shot map or clean per-team xG through this endpoint, so
 * this stops at lineups/events/team stats rather than chasing shot data
 * that isn't actually there.
 *
 * <p>Team/player resolution lives in {@link EspnTeamPlayerResolver} and the
 * lineup/event/stat writes live in {@link EspnMatchDetailPersister} -- both
 * split out into their own beans so a {@code @Transactional} boundary never
 * ends up wrapping the (rate-limited, potentially slow) ESPN HTTP calls.
 * Self-invocation within one class doesn't go through Spring's transactional
 * proxy, so this split is required, not just tidiness.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EspnLiveIngestionService {

    private static final String PROVIDER = "espn";

    // How far a scoreboard event's kickoff may drift from our own stored
    // kickoff and still be considered the same fixture -- generous enough to
    // absorb minor provider clock differences without risking a false match
    // against a different day's fixture between the same two teams.
    private static final Duration MATCH_WINDOW = Duration.ofHours(4);

    private final EspnClient client;
    private final ExternalRefResolver externalRefs;
    private final EspnTeamPlayerResolver teamPlayerResolver;
    private final EspnMatchDetailPersister matchDetailPersister;
    private final FixtureRepository fixtureRepository;

    @Transactional
    public void syncScores(Competition competition, String leagueSlug) {
        syncScores(competition, leagueSlug, null);
    }

    /**
     * Same as {@link #syncScores(Competition, String)} but for a specific
     * past date -- ESPN's scoreboard endpoint takes a {@code dates} param
     * and returns finished matches just as readily as today's, which is
     * what makes historical backfill via ESPN viable in the first place.
     */
    @Transactional
    public void syncScores(Competition competition, String leagueSlug, LocalDate date) {
        List<EspnScoreboardResponse.EventDto> events;
        try {
            events = client.getScoreboard(leagueSlug, date);
        } catch (EspnException e) {
            log.warn("ESPN scoreboard fetch failed for {} on {}: {}", leagueSlug, date, e.getMessage());
            return;
        }

        for (EspnScoreboardResponse.EventDto event : events) {
            if (event.competitions() == null || event.competitions().isEmpty()) {
                continue;
            }
            EspnScoreboardResponse.CompetitionDto comp = event.competitions().get(0);
            Fixture fixture = matchFixture(competition, event, comp);
            if (fixture == null) {
                continue;
            }

            externalRefs.link("FIXTURE", PROVIDER, event.id(), fixture.getId());
            fixture.setStatus(mapStatus(comp.status().type()));
            fixture.setHomeScore(findScore(comp.competitors(), "home"));
            fixture.setAwayScore(findScore(comp.competitors(), "away"));
            fixture.setUpdatedAt(Instant.now());
            fixtureRepository.save(fixture);
        }
    }

    /**
     * Deliberately NOT {@code @Transactional} at this level: the ESPN summary
     * fetch is rate-limited and can block for up to the limiter's 70s timeout
     * (three schedulers share one 20/min budget), and a single ambient
     * transaction here would hold a HikariCP connection checked out for that
     * entire wait -- starving the pool for actual web requests under load.
     * The external-ID lookup below is its own short transaction (a plain
     * repository call outside any ambient one auto-commits immediately), and
     * the actual DB writes happen in {@link EspnMatchDetailPersister}, a
     * separate bean, after the fetch completes.
     */
    public void syncMatchDetail(Fixture fixture, String leagueSlug) {
        String espnEventId = externalRefs.resolveExternalId("FIXTURE", PROVIDER, fixture.getId()).orElse(null);
        if (espnEventId == null) {
            return; // not yet seen by this competition's score sync
        }

        EspnSummaryResponse summary;
        try {
            summary = client.getSummary(leagueSlug, espnEventId);
        } catch (EspnException e) {
            log.warn("ESPN summary fetch failed for fixture {}: {}", fixture.getId(), e.getMessage());
            return;
        }

        matchDetailPersister.apply(fixture.getId(), fixture.getStatus(), summary);
    }

    private Fixture matchFixture(
            Competition competition, EspnScoreboardResponse.EventDto event, EspnScoreboardResponse.CompetitionDto comp) {
        Long existing = externalRefs.resolve("FIXTURE", PROVIDER, event.id()).orElse(null);
        if (existing != null) {
            return fixtureRepository.findById(existing).orElse(null);
        }

        Long homeTeamId = resolveTeam(competitor(comp, "home"));
        Long awayTeamId = resolveTeam(competitor(comp, "away"));
        if (homeTeamId == null || awayTeamId == null) {
            log.warn("ESPN event {} ({}) has unresolved team(s), skipping this cycle", event.id(), event.name());
            return null;
        }

        Instant kickoff = parseEspnDate(event.date());
        List<Fixture> matches = fixtureRepository.findByCompetitionIdAndHomeTeamIdAndAwayTeamIdAndUtcKickoffBetween(
                competition.getId(), homeTeamId, awayTeamId, kickoff.minus(MATCH_WINDOW), kickoff.plus(MATCH_WINDOW));
        return matches.isEmpty() ? null : matches.get(0);
    }

    private EspnScoreboardResponse.CompetitorDto competitor(EspnScoreboardResponse.CompetitionDto comp, String homeAway) {
        if (comp.competitors() == null) {
            return null;
        }
        return comp.competitors().stream()
                .filter(c -> homeAway.equals(c.homeAway()))
                .findFirst()
                .orElse(null);
    }

    private Long resolveTeam(EspnScoreboardResponse.CompetitorDto competitor) {
        if (competitor == null || competitor.team() == null) {
            return null;
        }
        return teamPlayerResolver.resolveTeamByRef(competitor.team().id(), competitor.team().displayName());
    }

    private Integer findScore(List<EspnScoreboardResponse.CompetitorDto> competitors, String homeAway) {
        if (competitors == null) {
            return null;
        }
        return competitors.stream()
                .filter(c -> homeAway.equals(c.homeAway()))
                .map(EspnScoreboardResponse.CompetitorDto::score)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .map(this::parseIntOrNull)
                .orElse(null);
    }

    /** Mirrors ESPN's generic pre/in/post status lifecycle onto our football-data.org-shaped enum. */
    private FixtureStatus mapStatus(EspnScoreboardResponse.StatusTypeDto type) {
        String state = type.state();
        String name = type.name() == null ? "" : type.name();
        if ("post".equals(state)) {
            if (name.contains("POSTPONED")) {
                return FixtureStatus.POSTPONED;
            }
            if (name.contains("CANCEL")) {
                return FixtureStatus.CANCELLED;
            }
            if (name.contains("ABANDON") || name.contains("SUSPEND")) {
                return FixtureStatus.SUSPENDED;
            }
            return FixtureStatus.FINISHED;
        }
        if ("in".equals(state)) {
            return name.contains("HALFTIME") ? FixtureStatus.PAUSED : FixtureStatus.IN_PLAY;
        }
        return FixtureStatus.SCHEDULED;
    }

    /** ESPN omits seconds ("2026-09-05T11:30Z") -- {@link Instant#parse} requires full ISO_INSTANT with seconds. */
    private Instant parseEspnDate(String date) {
        String normalized = date.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$")
                ? date.substring(0, date.length() - 1) + ":00Z"
                : date;
        return Instant.parse(normalized);
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureEvent;
import com.footpedia.domain.FixtureLineup;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.domain.FixtureTeamStat;
import com.footpedia.domain.Player;
import com.footpedia.domain.Team;
import com.footpedia.providers.espn.EspnClient;
import com.footpedia.providers.espn.EspnException;
import com.footpedia.providers.espn.dto.EspnScoreboardResponse;
import com.footpedia.providers.espn.dto.EspnSummaryResponse;
import com.footpedia.repository.FixtureEventRepository;
import com.footpedia.repository.FixtureLineupRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.FixtureTeamStatRepository;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+)");

    private final EspnClient client;
    private final ExternalRefResolver externalRefs;
    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final FixtureLineupRepository fixtureLineupRepository;
    private final FixtureEventRepository fixtureEventRepository;
    private final FixtureTeamStatRepository fixtureTeamStatRepository;

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

    @Transactional
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

        syncLineups(fixture.getId(), summary.rosters());
        syncEvents(fixture.getId(), summary.keyEvents());
        syncTeamStats(fixture.getId(), summary.boxscore());

        if (fixture.getStatus() == FixtureStatus.FINISHED) {
            fixture.setDetailFetchedAt(Instant.now());
            fixtureRepository.save(fixture);
        }
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
        return resolveTeamByRef(competitor.team().id(), competitor.team().displayName());
    }

    private Long resolveTeamByRef(String espnTeamId, String displayName) {
        Long existing = externalRefs.resolve("TEAM", PROVIDER, espnTeamId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Long matched = teamRepository.findAll().stream()
                .filter(t -> namesMatch(t.getName(), displayName) || namesMatch(t.getShortName(), displayName))
                .map(Team::getId)
                .findFirst()
                .orElse(null);
        if (matched != null) {
            externalRefs.link("TEAM", PROVIDER, espnTeamId, matched);
        }
        return matched;
    }

    private void syncLineups(Long fixtureId, List<EspnSummaryResponse.RosterDto> rosters) {
        if (rosters == null || rosters.isEmpty()) {
            return;
        }
        fixtureLineupRepository.deleteByFixtureId(fixtureId);
        for (EspnSummaryResponse.RosterDto roster : rosters) {
            if (roster.team() == null || roster.roster() == null) {
                continue;
            }
            Long teamId = resolveTeamByRef(roster.team().id(), roster.team().displayName());
            if (teamId == null) {
                continue;
            }
            for (EspnSummaryResponse.RosterEntryDto entry : roster.roster()) {
                Long playerId = resolveOrCreatePlayer(entry.athlete(), teamId);
                if (playerId == null) {
                    continue;
                }
                FixtureLineup lineup = new FixtureLineup();
                lineup.setFixtureId(fixtureId);
                lineup.setTeamId(teamId);
                lineup.setPlayerId(playerId);
                lineup.setPosition(entry.position() == null ? null : entry.position().abbreviation());
                lineup.setJerseyNumber(parseIntOrNull(entry.jersey()));
                lineup.setStarter(Boolean.TRUE.equals(entry.starter()));
                lineup.setSubbedIn(Boolean.TRUE.equals(entry.subbedIn()));
                lineup.setSubbedOut(Boolean.TRUE.equals(entry.subbedOut()));
                fixtureLineupRepository.save(lineup);
            }
        }
    }

    private void syncEvents(Long fixtureId, List<EspnSummaryResponse.KeyEventDto> keyEvents) {
        if (keyEvents == null) {
            return;
        }
        fixtureEventRepository.deleteByFixtureId(fixtureId);
        for (EspnSummaryResponse.KeyEventDto ke : keyEvents) {
            FixtureEvent fe = new FixtureEvent();
            fe.setFixtureId(fixtureId);
            fe.setTeamId(ke.team() == null ? null : resolveTeamByRef(ke.team().id(), ke.team().displayName()));

            List<EspnSummaryResponse.ParticipantDto> participants = ke.participants();
            if (participants != null && !participants.isEmpty()) {
                fe.setPlayerId(resolveOrCreatePlayer(participants.get(0).athlete(), fe.getTeamId()));
                if (participants.size() > 1) {
                    fe.setRelatedPlayerId(resolveOrCreatePlayer(participants.get(1).athlete(), fe.getTeamId()));
                }
            }
            fe.setEventType(ke.type() == null || ke.type().text() == null ? "UNKNOWN" : ke.type().text());
            fe.setMinute(parseMinute(ke.clock()));
            fe.setDetail(ke.text());
            fixtureEventRepository.save(fe);
        }
    }

    private void syncTeamStats(Long fixtureId, EspnSummaryResponse.BoxscoreDto boxscore) {
        if (boxscore == null || boxscore.teams() == null) {
            return;
        }
        for (EspnSummaryResponse.BoxscoreTeamDto bt : boxscore.teams()) {
            if (bt.team() == null || bt.statistics() == null) {
                continue;
            }
            Long teamId = resolveTeamByRef(bt.team().id(), bt.team().displayName());
            if (teamId == null) {
                continue;
            }
            for (EspnSummaryResponse.StatEntryDto stat : bt.statistics()) {
                if (stat.name() == null) {
                    continue;
                }
                FixtureTeamStat row = fixtureTeamStatRepository
                        .findByFixtureIdAndTeamIdAndStatType(fixtureId, teamId, stat.name())
                        .orElseGet(FixtureTeamStat::new);
                row.setFixtureId(fixtureId);
                row.setTeamId(teamId);
                row.setStatType(stat.name());
                row.setStatValue(stat.displayValue());
                fixtureTeamStatRepository.save(row);
            }
        }
    }

    private Long resolveOrCreatePlayer(EspnSummaryResponse.AthleteDto athlete, Long teamId) {
        if (athlete == null || athlete.id() == null) {
            return null;
        }
        Long existing = externalRefs.resolve("PLAYER", PROVIDER, athlete.id()).orElse(null);
        if (existing != null) {
            return existing;
        }
        Player player = new Player();
        player.setName(athlete.displayName());
        player.setCurrentTeamId(teamId);
        Long playerId = playerRepository.save(player).getId();
        externalRefs.link("PLAYER", PROVIDER, athlete.id(), playerId);
        return playerId;
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

    private Integer parseMinute(EspnSummaryResponse.ClockDto clock) {
        if (clock == null || clock.displayValue() == null) {
            return null;
        }
        Matcher m = LEADING_NUMBER.matcher(clock.displayValue().trim());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
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

    private boolean namesMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return normalizeTeamName(a).equals(normalizeTeamName(b));
    }

    private String normalizeTeamName(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("\\b(fc|afc|cf)\\b", "").replaceAll("[^a-z0-9]", "").trim();
    }
}

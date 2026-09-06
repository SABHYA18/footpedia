package com.footpedia.ingestion;

import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureEvent;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.domain.FixtureTeamStat;
import com.footpedia.providers.espn.dto.EspnSummaryResponse;
import com.footpedia.repository.FixtureEventRepository;
import com.footpedia.repository.FixtureLineupRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.FixtureTeamStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The DB-writing half of ESPN match detail. Split out from
 * {@link EspnLiveIngestionService} specifically so its {@code @Transactional}
 * boundary starts <em>after</em> the slow, rate-limited ESPN summary fetch
 * rather than wrapping it -- a transaction here only ever does fast local
 * writes, never blocks on network I/O while holding a DB connection.
 */
@Service
@RequiredArgsConstructor
class EspnMatchDetailPersister {

    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+)");

    private final EspnTeamPlayerResolver resolver;
    private final FixtureRepository fixtureRepository;
    private final FixtureLineupRepository fixtureLineupRepository;
    private final FixtureEventRepository fixtureEventRepository;
    private final FixtureTeamStatRepository fixtureTeamStatRepository;

    @Transactional
    void apply(Long fixtureId, FixtureStatus status, EspnSummaryResponse summary) {
        syncLineups(fixtureId, summary.rosters());
        syncEvents(fixtureId, summary.keyEvents());
        syncTeamStats(fixtureId, summary.boxscore());
        syncFormationsAndFinishedMarker(fixtureId, status, summary.rosters());
    }

    /**
     * Formation is a team-level attribute of the match itself (like the
     * score), not a per-player one, so it lives on Fixture rather than
     * fixture_lineup -- avoids repeating the same string across ~20 rows for
     * no benefit. Bundled with the detail_fetched_at update since both touch
     * the same Fixture row; no need for two separate fetch-then-saves.
     */
    private void syncFormationsAndFinishedMarker(
            Long fixtureId, FixtureStatus status, List<EspnSummaryResponse.RosterDto> rosters) {
        String homeFormation = null;
        String awayFormation = null;
        if (rosters != null) {
            for (EspnSummaryResponse.RosterDto roster : rosters) {
                if (roster.formation() == null) {
                    continue;
                }
                if ("home".equals(roster.homeAway())) {
                    homeFormation = roster.formation();
                } else if ("away".equals(roster.homeAway())) {
                    awayFormation = roster.formation();
                }
            }
        }

        boolean markFinished = status == FixtureStatus.FINISHED;
        if (homeFormation == null && awayFormation == null && !markFinished) {
            return;
        }

        Fixture fixture = fixtureRepository.findById(fixtureId).orElse(null);
        if (fixture == null) {
            return;
        }
        if (homeFormation != null) {
            fixture.setHomeFormation(homeFormation);
        }
        if (awayFormation != null) {
            fixture.setAwayFormation(awayFormation);
        }
        if (markFinished) {
            fixture.setDetailFetchedAt(Instant.now());
        }
        fixtureRepository.save(fixture);
    }

    /**
     * Atomic upsert by (fixture, player) rather than delete-then-reinsert or
     * even a JPA find-then-save -- this ran twice concurrently for the same
     * fixture in practice (a local test instance and the deployed one both
     * independently decided the same finished-hours-ago match still needed
     * backfilling), and a find-then-save still has a window where both see
     * "not found" for a genuinely new player and both try to insert (one of
     * them observed live, on a *different* fixture, after switching away
     * from delete-then-reinsert to find-then-save specifically to fix this).
     * {@code ON CONFLICT DO UPDATE} in {@link FixtureLineupRepository#upsert}
     * closes that window in Postgres itself.
     */
    private void syncLineups(Long fixtureId, List<EspnSummaryResponse.RosterDto> rosters) {
        if (rosters == null || rosters.isEmpty()) {
            return;
        }
        for (EspnSummaryResponse.RosterDto roster : rosters) {
            if (roster.team() == null || roster.roster() == null) {
                continue;
            }
            Long teamId = resolver.resolveTeamByRef(roster.team().id(), roster.team().displayName());
            if (teamId == null) {
                continue;
            }
            for (EspnSummaryResponse.RosterEntryDto entry : roster.roster()) {
                String position = entry.position() == null ? null : entry.position().abbreviation();
                // "SUB" is ESPN's bench marker, not an actual playing position -- fine to store
                // on the lineup row itself, but never as this player's Player.position.
                String playerPosition = "SUB".equals(position) ? null : position;
                Long playerId = resolver.resolveOrCreatePlayer(entry.athlete(), teamId, playerPosition);
                if (playerId == null) {
                    continue;
                }
                fixtureLineupRepository.upsert(
                        fixtureId,
                        teamId,
                        playerId,
                        position,
                        parseIntOrNull(entry.jersey()),
                        Boolean.TRUE.equals(entry.starter()),
                        Boolean.TRUE.equals(entry.subbedIn()),
                        Boolean.TRUE.equals(entry.subbedOut()),
                        parseFormationPlace(entry.formationPlace()));
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
            fe.setTeamId(ke.team() == null ? null : resolver.resolveTeamByRef(ke.team().id(), ke.team().displayName()));

            List<EspnSummaryResponse.ParticipantDto> participants = ke.participants();
            if (participants != null && !participants.isEmpty()) {
                fe.setPlayerId(resolver.resolveOrCreatePlayer(participants.get(0).athlete(), fe.getTeamId()));
                if (participants.size() > 1) {
                    fe.setRelatedPlayerId(resolver.resolveOrCreatePlayer(participants.get(1).athlete(), fe.getTeamId()));
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
            Long teamId = resolver.resolveTeamByRef(bt.team().id(), bt.team().displayName());
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

    /** ESPN sends "0" for substitutes/non-starters -- not a real slot, so treat it as absent. */
    private Integer parseFormationPlace(String s) {
        Integer n = parseIntOrNull(s);
        return n != null && n > 0 ? n : null;
    }
}

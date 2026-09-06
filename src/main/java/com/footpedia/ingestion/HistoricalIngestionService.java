package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixturePlayerStat;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.domain.FixtureTeamStat;
import com.footpedia.domain.Player;
import com.footpedia.domain.Team;
import com.footpedia.providers.apifootball.ApiFootballClient;
import com.footpedia.providers.apifootball.ApiFootballException;
import com.footpedia.providers.apifootball.dto.ApiFootballFixtureDto;
import com.footpedia.providers.apifootball.dto.ApiFootballPlayersResponse;
import com.footpedia.providers.apifootball.dto.ApiFootballStatsResponse;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.FixturePlayerStatRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.FixtureTeamStatRepository;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backfills historical (2022-2024 season) fixtures, team stats and per-player
 * stats from API-Football. Free-plan seasons only -- the live season already
 * comes from football-data.org via {@link FootballDataIngestionService}.
 *
 * <p>Not {@code @Transactional} at the top level: each repository write commits
 * as it happens, so a mid-run quota exhaustion keeps whatever was already
 * ingested instead of rolling all of it back.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalIngestionService {

    private static final String PROVIDER = "api-football";
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    private final ApiFootballClient client;
    private final ExternalRefResolver externalRefs;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final PlayerRepository playerRepository;
    private final FixtureTeamStatRepository fixtureTeamStatRepository;
    private final FixturePlayerStatRepository fixturePlayerStatRepository;

    public HistoricalIngestSummary ingestMatchday(String competitionCode, int leagueId, int season, int matchday) {
        Competition competition = competitionRepository.findByCode(competitionCode)
                .orElseThrow(() -> new IllegalStateException("Unknown competition: " + competitionCode));

        List<ApiFootballFixtureDto> fixtures;
        try {
            fixtures = client.getFixturesByRound(leagueId, season, "Regular Season - " + matchday);
        } catch (ApiFootballException e) {
            log.warn("Historical fixture list fetch failed: {}", e.getMessage());
            return new HistoricalIngestSummary(0, 0, 0);
        }

        int fixturesIngested = 0;
        int fixturesWithStats = 0;
        int playerRows = 0;

        for (ApiFootballFixtureDto dto : fixtures) {
            Long homeTeamId = resolveOrCreateTeam(dto.teams().home());
            Long awayTeamId = resolveOrCreateTeam(dto.teams().away());
            Long internalFixtureId = upsertFixture(competition, season, dto, homeTeamId, awayTeamId);
            fixturesIngested++;

            // Historical (finished) fixtures never change -- re-running a matchday to pick up
            // one new/fixed fixture must not re-spend quota re-fetching stats already stored.
            if (!fixtureTeamStatRepository.findByFixtureId(internalFixtureId).isEmpty()) {
                fixturesWithStats++;
                continue;
            }

            try {
                if (ingestTeamStats(dto.fixture().id(), internalFixtureId) > 0) {
                    fixturesWithStats++;
                }
                playerRows += ingestPlayerStats(dto.fixture().id(), internalFixtureId);
            } catch (ApiFootballException e) {
                log.warn("Stopping historical stats ingestion early: {}", e.getMessage());
                break;
            }
        }

        log.info(
                "Historical ingest for {} season {} matchday {}: {} fixtures, {} with team stats, {} player rows",
                competitionCode, season, matchday, fixturesIngested, fixturesWithStats, playerRows);
        return new HistoricalIngestSummary(fixturesIngested, fixturesWithStats, playerRows);
    }

    private Long upsertFixture(
            Competition competition, int season, ApiFootballFixtureDto dto, Long homeTeamId, Long awayTeamId) {
        Long fixtureId = externalRefs.resolve("FIXTURE", PROVIDER, String.valueOf(dto.fixture().id())).orElse(null);
        Fixture fixture = fixtureId == null ? new Fixture() : fixtureRepository.findById(fixtureId).orElseGet(Fixture::new);

        fixture.setCompetitionId(competition.getId());
        fixture.setSeason(String.valueOf(season));
        fixture.setMatchday(parseMatchday(dto.league().round()));
        fixture.setUtcKickoff(OffsetDateTime.parse(dto.fixture().date()).toInstant());
        fixture.setStatus(mapStatus(dto.fixture().status().shortStatus()));
        fixture.setHomeTeamId(homeTeamId);
        fixture.setAwayTeamId(awayTeamId);
        fixture.setHomeScore(dto.goals().home());
        fixture.setAwayScore(dto.goals().away());
        fixture.setUpdatedAt(Instant.now());
        fixture = fixtureRepository.save(fixture);

        externalRefs.link("FIXTURE", PROVIDER, String.valueOf(dto.fixture().id()), fixture.getId());
        return fixture.getId();
    }

    private int ingestTeamStats(long externalFixtureId, Long internalFixtureId) {
        List<ApiFootballStatsResponse.TeamStats> stats = client.getFixtureStatistics(externalFixtureId);
        int teamsWithStats = 0;
        for (ApiFootballStatsResponse.TeamStats teamStats : stats) {
            Long teamId = externalRefs.resolve("TEAM", PROVIDER, String.valueOf(teamStats.team().id())).orElse(null);
            if (teamId == null) {
                continue;
            }
            for (ApiFootballStatsResponse.StatEntry entry : teamStats.statistics()) {
                FixtureTeamStat stat = fixtureTeamStatRepository
                        .findByFixtureIdAndTeamIdAndStatType(internalFixtureId, teamId, entry.type())
                        .orElseGet(FixtureTeamStat::new);
                stat.setFixtureId(internalFixtureId);
                stat.setTeamId(teamId);
                stat.setStatType(entry.type());
                stat.setStatValue(entry.value() == null ? null : String.valueOf(entry.value()));
                fixtureTeamStatRepository.save(stat);
            }
            teamsWithStats++;
        }
        return teamsWithStats;
    }

    private int ingestPlayerStats(long externalFixtureId, Long internalFixtureId) {
        List<ApiFootballPlayersResponse.TeamPlayers> teams = client.getFixturePlayers(externalFixtureId);
        int rows = 0;
        for (ApiFootballPlayersResponse.TeamPlayers teamPlayers : teams) {
            Long teamId = externalRefs.resolve("TEAM", PROVIDER, String.valueOf(teamPlayers.team().id())).orElse(null);
            if (teamId == null) {
                continue;
            }

            for (ApiFootballPlayersResponse.PlayerEntry entry : teamPlayers.players()) {
                if (entry.statistics().isEmpty()) {
                    continue;
                }
                ApiFootballPlayersResponse.PlayerStats s = entry.statistics().get(0);
                if (s.games() == null || s.games().minutes() == null || s.games().minutes() == 0) {
                    continue; // did not feature
                }

                Long playerId = resolveOrCreatePlayer(entry.player(), teamId, s.games().position());

                FixturePlayerStat stat = fixturePlayerStatRepository
                        .findByFixtureIdAndPlayerId(internalFixtureId, playerId)
                        .orElseGet(FixturePlayerStat::new);
                stat.setFixtureId(internalFixtureId);
                stat.setTeamId(teamId);
                stat.setPlayerId(playerId);
                stat.setPosition(s.games().position());
                stat.setMinutes(s.games().minutes());
                stat.setRating(parseRating(s.games().rating()));
                stat.setGoals(s.goals() == null ? null : s.goals().total());
                stat.setAssists(s.goals() == null ? null : s.goals().assists());
                stat.setShotsTotal(s.shots() == null ? null : s.shots().total());
                stat.setShotsOn(s.shots() == null ? null : s.shots().on());
                stat.setPassesTotal(s.passes() == null ? null : s.passes().total());
                stat.setPassesAccuracy(s.passes() == null ? null : parseIntOrNull(s.passes().accuracy()));
                stat.setTackles(s.tackles() == null ? null : s.tackles().total());
                stat.setDuelsTotal(s.duels() == null ? null : s.duels().total());
                stat.setDuelsWon(s.duels() == null ? null : s.duels().won());
                stat.setYellowCards(s.cards() == null ? null : s.cards().yellow());
                stat.setRedCards(s.cards() == null ? null : s.cards().red());
                fixturePlayerStatRepository.save(stat);
                rows++;
            }
        }
        return rows;
    }

    private Long resolveOrCreateTeam(ApiFootballFixtureDto.TeamRef ref) {
        Long existing = externalRefs.resolve("TEAM", PROVIDER, String.valueOf(ref.id())).orElse(null);
        if (existing != null) {
            return existing;
        }

        Long matched = teamRepository.findAll().stream()
                .filter(t -> namesMatch(t.getName(), ref.name()) || namesMatch(t.getShortName(), ref.name()))
                .map(Team::getId)
                .findFirst()
                .orElse(null);

        Long teamId;
        if (matched != null) {
            teamId = matched;
        } else {
            Team team = new Team();
            team.setName(ref.name());
            team.setCrestUrl(ref.logo());
            teamId = teamRepository.save(team).getId();
        }
        externalRefs.link("TEAM", PROVIDER, String.valueOf(ref.id()), teamId);
        return teamId;
    }

    /**
     * {@code position} enriches an existing player only when its own position
     * is still null -- {@link com.footpedia.ingestion.EspnTeamPlayerResolver}
     * does the same on the ESPN side. Neither ingestion path used to write
     * this back to {@link Player} at all (only to the per-fixture stat/lineup
     * rows), leaving {@code Player.position} null for every squad member.
     */
    private Long resolveOrCreatePlayer(ApiFootballPlayersResponse.PlayerInfo info, Long teamId, String position) {
        Long existing = externalRefs.resolve("PLAYER", PROVIDER, String.valueOf(info.id())).orElse(null);
        if (existing != null) {
            enrichPositionIfMissing(existing, position);
            return existing;
        }

        Player player = new Player();
        player.setName(info.name());
        player.setPhotoUrl(info.photo());
        player.setCurrentTeamId(teamId);
        player.setPosition(position);
        Long playerId = playerRepository.save(player).getId();
        externalRefs.link("PLAYER", PROVIDER, String.valueOf(info.id()), playerId);
        return playerId;
    }

    private void enrichPositionIfMissing(Long playerId, String position) {
        if (position == null) {
            return;
        }
        playerRepository.findById(playerId).ifPresent(p -> {
            if (p.getPosition() == null) {
                p.setPosition(position);
                playerRepository.save(p);
            }
        });
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

    private Integer parseMatchday(String round) {
        if (round == null) {
            return null;
        }
        Matcher m = TRAILING_NUMBER.matcher(round.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** Mirrors API-Football's short status codes onto our football-data.org-shaped enum. */
    private FixtureStatus mapStatus(String shortStatus) {
        if (shortStatus == null) {
            return FixtureStatus.SCHEDULED;
        }
        return switch (shortStatus) {
            case "FT", "AET", "PEN" -> FixtureStatus.FINISHED;
            case "1H", "2H", "ET", "BT", "P", "LIVE" -> FixtureStatus.IN_PLAY;
            case "HT" -> FixtureStatus.PAUSED;
            case "PST" -> FixtureStatus.POSTPONED;
            case "SUSP", "INT" -> FixtureStatus.SUSPENDED;
            case "CANC", "ABD" -> FixtureStatus.CANCELLED;
            case "AWD", "WO" -> FixtureStatus.AWARDED;
            default -> FixtureStatus.SCHEDULED;
        };
    }

    private BigDecimal parseRating(String rating) {
        if (rating == null) {
            return null;
        }
        try {
            return new BigDecimal(rating);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

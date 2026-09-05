package com.footpedia.web.service;

import com.footpedia.domain.FixtureEvent;
import com.footpedia.domain.FixtureLineup;
import com.footpedia.domain.FixturePlayerStat;
import com.footpedia.domain.Player;
import com.footpedia.repository.FixtureEventRepository;
import com.footpedia.repository.FixtureLineupRepository;
import com.footpedia.repository.FixturePlayerStatRepository;
import com.footpedia.repository.FixtureTeamStatRepository;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.web.dto.FixtureStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixtureStatsQueryServiceImpl implements FixtureStatsQueryService {

    private final FixtureTeamStatRepository teamStatRepository;
    private final FixturePlayerStatRepository playerStatRepository;
    private final FixtureLineupRepository lineupRepository;
    private final FixtureEventRepository eventRepository;
    private final PlayerRepository playerRepository;

    @Override
    public FixtureStatsResponse getStats(Long fixtureId) {
        List<FixtureStatsResponse.TeamStatEntry> teamStats = teamStatRepository.findByFixtureId(fixtureId).stream()
                .map(s -> new FixtureStatsResponse.TeamStatEntry(s.getTeamId(), s.getStatType(), s.getStatValue()))
                .collect(Collectors.toList());

        List<FixturePlayerStat> playerRows = playerStatRepository.findByFixtureId(fixtureId);
        List<FixtureLineup> lineupRows = lineupRepository.findByFixtureId(fixtureId);
        List<FixtureEvent> eventRows = eventRepository.findByFixtureIdOrderByMinuteAsc(fixtureId);

        Set<Long> playerIds = Stream.of(
                        playerRows.stream().map(FixturePlayerStat::getPlayerId),
                        lineupRows.stream().map(FixtureLineup::getPlayerId),
                        eventRows.stream().map(FixtureEvent::getPlayerId),
                        eventRows.stream().map(FixtureEvent::getRelatedPlayerId))
                .flatMap(Function.identity())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Player> playersById = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        List<FixtureStatsResponse.PlayerStatEntry> playerStats = playerRows.stream()
                .map(p -> {
                    Player player = playersById.get(p.getPlayerId());
                    return new FixtureStatsResponse.PlayerStatEntry(
                            p.getTeamId(),
                            p.getPlayerId(),
                            player == null ? "Unknown" : player.getName(),
                            player == null ? null : player.getPhotoUrl(),
                            p.getPosition(),
                            p.getMinutes(),
                            p.getRating(),
                            p.getGoals(),
                            p.getAssists(),
                            p.getShotsTotal(),
                            p.getShotsOn(),
                            p.getPassesTotal(),
                            p.getPassesAccuracy(),
                            p.getTackles(),
                            p.getDuelsTotal(),
                            p.getDuelsWon(),
                            p.getYellowCards(),
                            p.getRedCards());
                })
                .collect(Collectors.toList());

        List<FixtureStatsResponse.LineupEntry> lineups = lineupRows.stream()
                .map(l -> {
                    Player player = playersById.get(l.getPlayerId());
                    return new FixtureStatsResponse.LineupEntry(
                            l.getTeamId(),
                            l.getPlayerId(),
                            player == null ? "Unknown" : player.getName(),
                            l.getPosition(),
                            l.getJerseyNumber(),
                            l.isStarter(),
                            l.isSubbedIn(),
                            l.isSubbedOut(),
                            l.getFormationPlace());
                })
                .collect(Collectors.toList());

        List<FixtureStatsResponse.MatchEventEntry> events = eventRows.stream()
                .map(e -> {
                    Player player = playersById.get(e.getPlayerId());
                    Player relatedPlayer = playersById.get(e.getRelatedPlayerId());
                    return new FixtureStatsResponse.MatchEventEntry(
                            e.getTeamId(),
                            e.getPlayerId(),
                            player == null ? null : player.getName(),
                            e.getRelatedPlayerId(),
                            relatedPlayer == null ? null : relatedPlayer.getName(),
                            e.getEventType(),
                            e.getMinute(),
                            e.getDetail());
                })
                .collect(Collectors.toList());

        return new FixtureStatsResponse(teamStats, playerStats, lineups, events);
    }
}

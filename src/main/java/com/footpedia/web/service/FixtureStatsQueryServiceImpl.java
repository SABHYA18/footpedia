package com.footpedia.web.service;

import com.footpedia.domain.FixturePlayerStat;
import com.footpedia.domain.Player;
import com.footpedia.repository.FixturePlayerStatRepository;
import com.footpedia.repository.FixtureTeamStatRepository;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.web.dto.FixtureStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixtureStatsQueryServiceImpl implements FixtureStatsQueryService {

    private final FixtureTeamStatRepository teamStatRepository;
    private final FixturePlayerStatRepository playerStatRepository;
    private final PlayerRepository playerRepository;

    @Override
    public FixtureStatsResponse getStats(Long fixtureId) {
        List<FixtureStatsResponse.TeamStatEntry> teamStats = teamStatRepository.findByFixtureId(fixtureId).stream()
                .map(s -> new FixtureStatsResponse.TeamStatEntry(s.getTeamId(), s.getStatType(), s.getStatValue()))
                .collect(Collectors.toList());

        List<FixturePlayerStat> playerRows = playerStatRepository.findByFixtureId(fixtureId);
        Map<Long, Player> playersById = playerRepository
                .findAllById(playerRows.stream().map(FixturePlayerStat::getPlayerId).distinct().toList())
                .stream()
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

        return new FixtureStatsResponse(teamStats, playerStats);
    }
}

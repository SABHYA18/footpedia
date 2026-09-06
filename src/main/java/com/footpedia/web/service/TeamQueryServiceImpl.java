package com.footpedia.web.service;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Player;
import com.footpedia.domain.Standing;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.repository.StandingRepository;
import com.footpedia.repository.TeamRepository;
import com.footpedia.web.dto.PlayerResponse;
import com.footpedia.web.dto.TeamResponse;
import com.footpedia.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamQueryServiceImpl implements TeamQueryService {

    private final CompetitionLookup competitionLookup;
    private final TeamRepository teamRepository;
    private final StandingRepository standingRepository;
    private final PlayerRepository playerRepository;

    @Override
    public TeamResponse getById(Long id) {
        return teamRepository
                .findById(id)
                .map(TeamResponse::from)
                .orElseThrow(() -> new NotFoundException("Unknown team: " + id));
    }

    @Override
    public List<TeamResponse> getByCompetition(String competitionCode) {
        Competition competition = competitionLookup.byCode(competitionCode);
        List<Long> teamIds = standingRepository
                .findByCompetitionIdAndSeasonOrderByPositionAsc(competition.getId(), competition.getCurrentSeason())
                .stream()
                .map(Standing::getTeamId)
                .toList();

        return teamRepository.findAllById(teamIds).stream().map(TeamResponse::from).toList();
    }

    /**
     * De-duplicates by name before returning. API-Football's historical
     * backfill and ESPN's live-season sync each maintain their own
     * external_ref namespace with no cross-provider reconciliation, so any
     * player who appears in both got two separate Player rows before
     * {@code EspnTeamPlayerResolver} started checking for an existing one by
     * name -- 107 duplicate (team, name) groups were already in the database
     * for PL alone by the time that fix landed. That fix only stops new
     * duplicates; this merges the ones already there for display, preferring
     * whichever row has more filled-in fields, without touching the
     * underlying rows (a real merge would mean redirecting every
     * fixture_lineup/fixture_event/fixture_player_stat reference too -- more
     * than this feature needs).
     */
    @Override
    public List<PlayerResponse> getPlayersForTeam(Long teamId) {
        Map<String, Player> byName = new LinkedHashMap<>();
        for (Player p : playerRepository.findByCurrentTeamIdOrderByNameAsc(teamId)) {
            String key = normalizePlayerName(p.getName());
            Player current = byName.get(key);
            if (current == null || completeness(p) > completeness(current)) {
                byName.put(key, p);
            }
        }
        return byName.values().stream()
                .sorted(Comparator.comparing(Player::getName))
                .map(PlayerResponse::from)
                .toList();
    }

    private int completeness(Player p) {
        int score = 0;
        if (p.getPhotoUrl() != null) {
            score++;
        }
        if (p.getPosition() != null) {
            score++;
        }
        if (p.getNationality() != null) {
            score++;
        }
        if (p.getDateOfBirth() != null) {
            score++;
        }
        return score;
    }

    private String normalizePlayerName(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}

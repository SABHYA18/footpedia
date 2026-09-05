package com.footpedia.ingestion;

import com.footpedia.domain.Player;
import com.footpedia.domain.Team;
import com.footpedia.providers.espn.dto.EspnSummaryResponse;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Team/player identity resolution shared by {@link EspnLiveIngestionService}
 * (score/fixture matching) and {@link EspnMatchDetailPersister}
 * (lineups/events/stats) -- split out because both need it and neither
 * should duplicate the name-matching logic.
 */
@Component
@RequiredArgsConstructor
class EspnTeamPlayerResolver {

    private static final String PROVIDER = "espn";

    private final ExternalRefResolver externalRefs;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    Long resolveTeamByRef(String espnTeamId, String displayName) {
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

    Long resolveOrCreatePlayer(EspnSummaryResponse.AthleteDto athlete, Long teamId) {
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

package com.footpedia.ingestion;

import com.footpedia.domain.Player;
import com.footpedia.domain.Team;
import com.footpedia.providers.espn.dto.EspnSummaryResponse;
import com.footpedia.repository.PlayerRepository;
import com.footpedia.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    /**
     * football-data.org's official name -> ESPN's name, for the handful of
     * non-English clubs no amount of punctuation/digit stripping can bridge
     * -- ESPN anglicizes city names ("München" -> "Munich", "Köln" ->
     * "Cologne") or drops "Club ... de" -style wording entirely ("Club
     * Atlético de Madrid" -> "Atlético Madrid"). Found by diffing
     * football-data.org's team list against ESPN's for BL1/PD/SA -- these
     * were the only 7 of ~58 teams across those three leagues that didn't
     * already resolve via {@link #normalizeTeamName}.
     *
     * <p>Keys/values are run through {@link #normalizeTeamName} once, up
     * front, rather than compared as raw strings -- accented characters can
     * arrive as either precomposed ("ö", one code point) or decomposed ("o"
     * + a combining diaeresis, two code points) depending on what produced
     * the string, and a raw {@code Map.get} on the literal source text would
     * silently miss if this class's own source encoding didn't happen to
     * match whatever the database/JSON used.
     */
    private static final Map<String, String> KNOWN_ALIASES = buildAliasMap();

    private static Map<String, String> buildAliasMap() {
        Map<String, String> raw = Map.ofEntries(
                Map.entry("1. FC Köln", "FC Cologne"),
                Map.entry("FC Bayern München", "Bayern Munich"),
                Map.entry("Hamburger SV", "Hamburg SV"),
                Map.entry("Club Atlético de Madrid", "Atlético Madrid"),
                Map.entry("RC Celta de Vigo", "Celta Vigo"),
                Map.entry("Real Racing Club de Santander", "Racing Santander"),
                Map.entry("FC Internazionale Milano", "Internazionale"));
        Map<String, String> normalized = new HashMap<>();
        raw.forEach((ours, espn) -> normalized.put(normalizeTeamName(ours), normalizeTeamName(espn)));
        return normalized;
    }

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

    /** {@code ours} is always our (football-data.org) name/short-name; {@code espn} is ESPN's displayName. */
    private boolean namesMatch(String ours, String espn) {
        if (ours == null || espn == null) {
            return false;
        }
        String normalizedOurs = normalizeTeamName(ours);
        String normalizedEspn = normalizeTeamName(espn);
        if (normalizedOurs.equals(normalizedEspn)) {
            return true;
        }
        String alias = KNOWN_ALIASES.get(normalizedOurs);
        return alias != null && alias.equals(normalizedEspn);
    }

    /**
     * Strips diacritics (so "München"/"Múnich"-style accent differences
     * alone don't cause a miss), "FC"/"SV"-style club-type prefixes, and
     * standalone digit groups (founding-year/season suffixes many German and
     * Italian clubs carry in their official name -- "Bayer 04 Leverkusen",
     * "TSG 1899 Hoffenheim", "Como 1907" -- that ESPN's shorter names omit).
     */
    private static String normalizeTeamName(String s) {
        String withoutDiacritics = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\b(fc|afc|cf|sv)\\b", "")
                .replaceAll("\\b\\d+\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}

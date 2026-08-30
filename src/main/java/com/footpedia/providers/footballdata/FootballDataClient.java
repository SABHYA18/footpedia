package com.footpedia.providers.footballdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footpedia.ingestion.RawPayloadStore;
import com.footpedia.providers.footballdata.dto.CompetitionDto;
import com.footpedia.providers.footballdata.dto.MatchDto;
import com.footpedia.providers.footballdata.dto.MatchListResponse;
import com.footpedia.providers.footballdata.dto.StandingsResponse;
import com.footpedia.providers.footballdata.dto.TeamDto;
import com.footpedia.providers.footballdata.dto.TeamListResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Thin client over football-data.org v4. Every response is stored raw
 * ({@link RawPayloadStore}) before being parsed. Rate-limited to the free
 * plan's 10 req/min, which has no daily cap, so this is the polling
 * workhorse for fixtures/standings/scorers.
 */
@Component
@RequiredArgsConstructor
public class FootballDataClient {

    private static final String PROVIDER = "football-data";

    private final WebClient footballDataWebClient;
    private final ObjectMapper objectMapper;
    private final RawPayloadStore rawPayloadStore;

    @RateLimiter(name = PROVIDER)
    public CompetitionDto getCompetition(String competitionCode) {
        return parse(fetch("/competitions/" + competitionCode), CompetitionDto.class);
    }

    @RateLimiter(name = PROVIDER)
    public List<TeamDto> getTeams(String competitionCode) {
        return parse(fetch("/competitions/" + competitionCode + "/teams"), TeamListResponse.class).teams();
    }

    @RateLimiter(name = PROVIDER)
    public List<MatchDto> getMatches(String competitionCode) {
        return parse(fetch("/competitions/" + competitionCode + "/matches"), MatchListResponse.class).matches();
    }

    @RateLimiter(name = PROVIDER)
    public List<StandingsResponse.TableRowDto> getStandings(String competitionCode) {
        StandingsResponse response = parse(
                fetch("/competitions/" + competitionCode + "/standings"), StandingsResponse.class);
        return response.standings().stream()
                .filter(group -> "TOTAL".equals(group.type()))
                .findFirst()
                .map(StandingsResponse.StandingGroup::table)
                .orElse(List.of());
    }

    private String fetch(String path) {
        FetchResult result = footballDataWebClient.get()
                .uri(path)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new FetchResult(response.statusCode().value(), body)))
                .block();

        rawPayloadStore.store(PROVIDER, path, null, result.status(), result.body());

        if (result.status() >= 400) {
            throw new FootballDataException(
                    "football-data.org returned %d for %s".formatted(result.status(), path));
        }
        return result.body();
    }

    private <T> T parse(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new FootballDataException("Failed to parse football-data.org response", e);
        }
    }

    private record FetchResult(int status, String body) {
    }
}

package com.footpedia.providers.apifootball;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footpedia.config.ApiFootballProperties;
import com.footpedia.ingestion.RawPayloadStore;
import com.footpedia.providers.apifootball.dto.ApiFootballFixtureDto;
import com.footpedia.providers.apifootball.dto.ApiFootballFixtureListResponse;
import com.footpedia.providers.apifootball.dto.ApiFootballPlayersResponse;
import com.footpedia.providers.apifootball.dto.ApiFootballStatsResponse;
import com.footpedia.quota.ProviderQuotaGuard;
import com.footpedia.quota.QuotaDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Thin client over API-Football v3. Unlike {@code FootballDataClient}, every
 * call first asks {@link ProviderQuotaGuard} for permission -- the free plan's
 * 100/day budget makes an unmetered call unacceptable.
 */
@Component
@RequiredArgsConstructor
public class ApiFootballClient {

    private static final String PROVIDER = "api-football";

    private final WebClient apiFootballWebClient;
    private final ObjectMapper objectMapper;
    private final RawPayloadStore rawPayloadStore;
    private final ProviderQuotaGuard quotaGuard;
    private final ApiFootballProperties properties;

    // Free-plan gotcha: a date-range window can silently miss fixtures that kick off
    // outside it (e.g. a Monday-night match after a Fri-Sun window). round= returns
    // the exact matchday with no gaps, and (unlike last=) isn't blocked on Free.
    public List<ApiFootballFixtureDto> getFixturesByRound(int leagueId, int season, String round) {
        String params = "league=%d&season=%d&round=%s"
                .formatted(leagueId, season, URLEncoder.encode(round, StandardCharsets.UTF_8));
        return parse(fetch("/fixtures", params), ApiFootballFixtureListResponse.class).response();
    }

    public List<ApiFootballStatsResponse.TeamStats> getFixtureStatistics(long fixtureId) {
        return parse(fetch("/fixtures/statistics", "fixture=" + fixtureId), ApiFootballStatsResponse.class).response();
    }

    public List<ApiFootballPlayersResponse.TeamPlayers> getFixturePlayers(long fixtureId) {
        return parse(fetch("/fixtures/players", "fixture=" + fixtureId), ApiFootballPlayersResponse.class).response();
    }

    private String fetch(String path, String queryParams) {
        QuotaDecision decision = quotaGuard.tryAcquire(
                PROVIDER, properties.requestsPerDay(), properties.requestsPerMinute(), properties.reserveThreshold());
        if (!decision.allowed()) {
            throw new ApiFootballException("api-football quota exhausted: " + decision.reason());
        }

        String uri = path + "?" + queryParams;
        FetchResult result = apiFootballWebClient.get()
                .uri(uri)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new FetchResult(response.statusCode().value(), body)))
                .block();

        rawPayloadStore.store(PROVIDER, path, queryParams, result.status(), result.body());

        if (result.status() >= 400) {
            throw new ApiFootballException("api-football returned %d for %s".formatted(result.status(), uri));
        }
        return result.body();
    }

    private <T> T parse(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new ApiFootballException("Failed to parse api-football response", e);
        }
    }

    private record FetchResult(int status, String body) {
    }
}

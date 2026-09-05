package com.footpedia.providers.espn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footpedia.ingestion.RawPayloadStore;
import com.footpedia.providers.espn.dto.EspnScoreboardResponse;
import com.footpedia.providers.espn.dto.EspnSummaryResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Thin client over ESPN's unofficial site API. Every response is stored raw
 * ({@link RawPayloadStore}) before being parsed, same as the documented
 * providers -- if ESPN reshapes an undocumented field, the raw payload lets
 * parsing be fixed and replayed at zero extra cost.
 */
@Component
@RequiredArgsConstructor
public class EspnClient {

    private static final String PROVIDER = "espn";
    private static final DateTimeFormatter DATE_PARAM = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient espnWebClient;
    private final ObjectMapper objectMapper;
    private final RawPayloadStore rawPayloadStore;

    /** Today's scoreboard. */
    public List<EspnScoreboardResponse.EventDto> getScoreboard(String leagueSlug) {
        return getScoreboard(leagueSlug, null);
    }

    /**
     * Scoreboard for a specific date -- confirmed working against ESPN's live
     * endpoint for past dates too. Its own rate-limiter bucket, separate from
     * {@link #getSummary}: {@code EspnLiveScoreScheduler} calls this every
     * 30s across up to 5 leagues and was found (live) to consume nearly the
     * entire shared 20/min budget by itself, starving summary calls almost
     * completely -- in one observed 15-minute window, 56 scoreboard calls
     * went out against just 1 summary call, stalling the historical
     * backfill's formation-place rollout to a crawl. Splitting the budget by
     * concern guarantees the backfill some throughput regardless of how busy
     * live polling is.
     */
    @RateLimiter(name = "espn-scoreboard")
    public List<EspnScoreboardResponse.EventDto> getScoreboard(String leagueSlug, LocalDate date) {
        String path = "/" + leagueSlug + "/scoreboard" + (date == null ? "" : "?dates=" + date.format(DATE_PARAM));
        EspnScoreboardResponse response = parse(fetch(path), EspnScoreboardResponse.class);
        return response.events() == null ? List.of() : response.events();
    }

    @RateLimiter(name = "espn-summary")
    public EspnSummaryResponse getSummary(String leagueSlug, String eventId) {
        return parse(fetch("/" + leagueSlug + "/summary?event=" + eventId), EspnSummaryResponse.class);
    }

    private String fetch(String path) {
        FetchResult result = espnWebClient.get()
                .uri(path)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new FetchResult(response.statusCode().value(), body)))
                .block();

        rawPayloadStore.store(PROVIDER, path, null, result.status(), result.body());

        if (result.status() >= 400) {
            throw new EspnException("ESPN returned %d for %s".formatted(result.status(), path));
        }
        return result.body();
    }

    private <T> T parse(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new EspnException("Failed to parse ESPN response", e);
        }
    }

    private record FetchResult(int status, String body) {
    }
}

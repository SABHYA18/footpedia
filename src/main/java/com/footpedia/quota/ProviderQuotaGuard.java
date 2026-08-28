package com.footpedia.quota;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Atomic quota guard for the upstream football providers.
 *
 * <p>API-Football's free plan allows 100 requests per DAY, which means a naive
 * "fetch on page view" design is exhausted by roughly a hundred page views and
 * then permanently broken. Every outbound call therefore has to ask permission
 * here first.
 *
 * <p>Two independent limits are enforced in a single round trip:
 * <ul>
 *   <li>a per-minute ceiling, which both providers impose; and</li>
 *   <li>a per-day budget, which only API-Football imposes.</li>
 * </ul>
 *
 * <p>{@code reserve} carves out the tail of the daily budget so that
 * discretionary work (a user idly browsing player pages) can never starve the
 * non-negotiable work (refreshing scores for fixtures kicking off tonight).
 * Scheduled jobs pass {@code reserve = 0} and may spend down to zero; lazy,
 * user-triggered fetches pass the configured threshold and are cut off early.
 *
 * <p>The INCR-then-EXPIRE pair must be atomic — doing it in two Java round
 * trips leaves a window where a crash strands a counter with no TTL, which
 * would wedge the provider until someone deleted the key by hand.
 */
@Component
public class ProviderQuotaGuard {

    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local dayKey    = KEYS[1]
            local minuteKey = KEYS[2]
            local dayLimit    = tonumber(ARGV[1])
            local minuteLimit = tonumber(ARGV[2])
            local reserve     = tonumber(ARGV[3])

            local dayUsed    = tonumber(redis.call('GET', dayKey))    or 0
            local minuteUsed = tonumber(redis.call('GET', minuteKey)) or 0

            -- Check both ceilings BEFORE incrementing, so a refused call
            -- does not itself consume budget.
            if minuteUsed >= minuteLimit then
                return {0, 2, dayLimit - dayUsed}
            end
            if dayUsed >= (dayLimit - reserve) then
                return {0, 1, dayLimit - dayUsed}
            end

            local newDay = redis.call('INCR', dayKey)
            if newDay == 1 then
                -- 48h: comfortably outlives the UTC day without unbounded growth.
                redis.call('EXPIRE', dayKey, 172800)
            end
            local newMinute = redis.call('INCR', minuteKey)
            if newMinute == 1 then
                redis.call('EXPIRE', minuteKey, 120)
            end

            return {1, 0, dayLimit - newDay}
            """, List.class);

    private final StringRedisTemplate redis;

    public ProviderQuotaGuard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Attempts to reserve one request against a provider's budget.
     *
     * @param provider    provider key, e.g. {@code api-football}
     * @param dayLimit    requests permitted per UTC day
     * @param minuteLimit requests permitted per minute
     * @param reserve     tail of the daily budget to withhold from this caller
     */
    public QuotaDecision tryAcquire(String provider, int dayLimit, int minuteLimit, int reserve) {
        String dayKey = "quota:%s:day:%s".formatted(provider, LocalDate.now(ZoneOffset.UTC));
        String minuteKey = "quota:%s:min:%d".formatted(provider, System.currentTimeMillis() / 60_000);

        List<?> result = redis.execute(
                SCRIPT,
                List.of(dayKey, minuteKey),
                String.valueOf(dayLimit),
                String.valueOf(minuteLimit),
                String.valueOf(reserve));

        if (result == null || result.size() < 3) {
            // Redis unreachable: fail CLOSED. Spending unmetered quota against a
            // 100/day budget is worse than briefly serving stale data.
            return QuotaDecision.deny("quota-backend-unavailable", 0);
        }

        long allowed = ((Number) result.get(0)).longValue();
        long reasonCode = ((Number) result.get(1)).longValue();
        long dayRemaining = ((Number) result.get(2)).longValue();

        if (allowed == 1) {
            return QuotaDecision.allow(dayRemaining);
        }
        String reason = reasonCode == 2 ? "minute-limit-exceeded" : "daily-budget-exhausted";
        return QuotaDecision.deny(reason, dayRemaining);
    }

    /** Requests left in today's budget, without consuming any. */
    public long remainingToday(String provider, int dayLimit) {
        String dayKey = "quota:%s:day:%s".formatted(provider, LocalDate.now(ZoneOffset.UTC));
        String used = redis.opsForValue().get(dayKey);
        return dayLimit - (used == null ? 0 : Long.parseLong(used));
    }
}

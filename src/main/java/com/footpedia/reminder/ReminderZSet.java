package com.footpedia.reminder;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Redis ZSET of pending reminders, scored by their due (notify_at) instant.
 * The DB `reminder` table is the durable record; this index exists purely so
 * the poller can ask "what's due?" in O(log n) instead of scanning the table
 * every tick.
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderZSet {

    // Atomically read-then-remove the due members, so two overlapping poll
    // ticks can't both claim (and double-send) the same reminder.
    private static final DefaultRedisScript<List> POP_DUE_SCRIPT = new DefaultRedisScript<>("""
            local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            if #due > 0 then
                redis.call('ZREM', KEYS[1], unpack(due))
            end
            return due
            """, List.class);

    private final StringRedisTemplate redis;
    private final ReminderProperties properties;

    public void schedule(Long reminderId, Instant notifyAt) {
        redis.opsForZSet().add(properties.zsetKey(), String.valueOf(reminderId), notifyAt.getEpochSecond());
    }

    public void cancel(Long reminderId) {
        redis.opsForZSet().remove(properties.zsetKey(), String.valueOf(reminderId));
    }

    /** Atomically pops every reminder id due at or before {@code now}. */
    @SuppressWarnings("unchecked")
    public List<Long> popDue(Instant now) {
        List<String> due = redis.execute(
                POP_DUE_SCRIPT, Collections.singletonList(properties.zsetKey()), String.valueOf(now.getEpochSecond()));
        return due == null ? List.of() : due.stream().map(Long::valueOf).toList();
    }
}

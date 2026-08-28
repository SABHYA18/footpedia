package com.footpedia.quota;

/**
 * Outcome of asking the guard for permission to spend one provider request.
 *
 * @param allowed        whether the call may proceed
 * @param reason         why it was refused (null when allowed)
 * @param dayRemaining   requests left in today's budget after this decision
 */
public record QuotaDecision(boolean allowed, String reason, long dayRemaining) {

    public static QuotaDecision allow(long dayRemaining) {
        return new QuotaDecision(true, null, dayRemaining);
    }

    public static QuotaDecision deny(String reason, long dayRemaining) {
        return new QuotaDecision(false, reason, dayRemaining);
    }
}

package com.footpedia.config;

/** Cache names, matching the TTL table in {@code footpedia.cache-ttl}. */
public final class CacheNames {

    public static final String LIVE_SCORE = "live-score";
    public static final String TODAYS_FIXTURES = "todays-fixtures";
    public static final String FIXTURE_LIST = "fixture-list";
    public static final String STANDINGS = "standings";
    public static final String PLAYER_SEASON_STATS = "player-season-stats";
    public static final String TRANSFERS = "transfers";
    public static final String HISTORICAL_FIXTURE = "historical-fixture";

    private CacheNames() {
    }
}
